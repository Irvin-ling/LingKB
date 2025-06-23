package com.ling.lingkb.data.parser;

import com.ling.lingkb.common.entity.DocumentParseResult;
import com.ling.lingkb.common.exception.DocumentParseException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Document Parser Factory
 * <p>
 * Responsible for assigning appropriate parsers based on file types, supporting automatic type detection
 * and batch parsing operations.
 * </p>
 *
 * @author shipotian
 * @since 1.0.0
 */
@Slf4j
@Component
public class DocumentParserFactory {

    private final List<DocumentParser> parsers;
    private final Map<String, DocumentParser> parserCache = new ConcurrentHashMap<>();
    private final Tika tika = new Tika();

    @Autowired
    public DocumentParserFactory(List<DocumentParser> parsers) {
        this.parsers = parsers.stream().sorted(Comparator.comparingInt(DocumentParser::getPriority))
                .collect(Collectors.toList());
    }

    /**
     * Gets the appropriate parser for the given file extension (with caching)
     *
     * @param extension the file extension to look up
     * @return the document parser instance
     * @throws DocumentParseException if no parser is found for the extension
     */
    public DocumentParser getParser(String extension) {
        String lowerExt = extension.toLowerCase();
        return parserCache.computeIfAbsent(lowerExt, ext -> parsers.stream().filter(p -> p.supports(ext)).findFirst()
                .orElseThrow(() -> new DocumentParseException("No parser found for extension: " + ext)));
    }

    /**
     * Automatically detects file type and parses the document
     *
     * @param file the file to parse
     * @return the parsing result
     * @throws DocumentParseException if parsing fails
     */
    public DocumentParseResult parse(File file) throws DocumentParseException {
        try {
            Path path = file.toPath();
            String extension = getFileExtension(path);
            DocumentParser parser = getParser(extension);
            log.debug("Using parser [{}] for file: {}", parser.getClass().getSimpleName(), file.getName());
            return parser.parse(path);
        } catch (Exception e) {
            throw new DocumentParseException("Document parsing failed: " + file.getName(), e);
        }
    }

    /**
     * Batch parses files/directories
     *
     * @param file the file or directory to parse
     * @return list of parsing results
     * @throws DocumentParseException if batch parsing fails
     */
    public List<DocumentParseResult> batchParse(File file) throws DocumentParseException {
        try {
            if (file.isDirectory()) {
                return Arrays.stream(Objects.requireNonNull(file.listFiles())).parallel().flatMap(f -> {
                    try {
                        return batchParse(f).stream();
                    } catch (DocumentParseException e) {
                        log.warn("Failed to parse file: {}", f.getName(), e);
                        return Stream.empty();
                    }
                }).collect(Collectors.toList());
            }
            return Collections.singletonList(parse(file));
        } catch (Exception e) {
            throw new DocumentParseException("Batch parse failed for: " + file.getName(), e);
        }
    }

    /**
     * Gets the file extension (supports MIME type detection for extensionless files)
     *
     * @param path the file path
     * @return the detected file extension
     * @throws MimeTypeException if MIME type detection fails
     */
    private String getFileExtension(Path path) throws MimeTypeException, IOException {
        String filename = path.getFileName().toString();
        String extension = FilenameUtils.getExtension(filename);

        if (StringUtils.isBlank(extension)) {
            String mimeType = tika.detect(path);
            extension = MimeTypes.getDefaultMimeTypes().forName(mimeType).getExtension();
            extension = StringUtils.removeStart(extension, ".");
            log.info("Detected file type: {} for {}", extension, filename);
        }

        return extension.toLowerCase();
    }

    /**
     * Gets all supported file types
     *
     * @return set of supported file extensions
     */
    public Set<String> getSupportedTypes() {
        return parsers.stream().flatMap(p -> p.supportedTypes().stream()).collect(Collectors.toSet());
    }
}