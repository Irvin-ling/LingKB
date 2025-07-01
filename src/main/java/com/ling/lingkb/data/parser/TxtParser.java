package com.ling.lingkb.data.parser;

import com.ling.lingkb.entity.DocumentParseResult;
import com.ling.lingkb.exception.DocumentParseException;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Text Document Parser
 * <p>
 * Parses plain text files (TXT) and extracts content with metadata
 * </p>
 *
 * @author shipotian
 * @version 1.0.0
 */
@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "data.parser.txt")
public class TxtParser implements DocumentParser {
    private int maxTextLength = 10_000_000;
    private int maxLines = 100_000;

    @Override
    public DocumentParseResult parse(Path filePath) throws DocumentParseException {
        log.info("TxtParser.parse({})...", filePath);
        String fileName = filePath.getFileName().toString();
        DocumentParseResult result = new DocumentParseResult();

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            StringBuilder textBuilder = new StringBuilder();
            String line;
            int lineCount = 0;
            long charCount = 0;

            while ((line = reader.readLine()) != null && lineCount < maxLines) {
                if (charCount + line.length() > maxTextLength) {
                    log.warn("current file content truncated due to size limit {}", maxTextLength);
                    textBuilder.append("...[content truncated due to size limit]");
                    break;
                }

                textBuilder.append(line).append("\n");
                lineCount++;
                charCount += line.length();
            }

            result.setText(textBuilder.toString());
            result.setMetadata(DocumentParseResult.DocumentMetadata.builder().author("Unknown").sourceFileName(fileName)
                    .creationDate(Files.getLastModifiedTime(filePath).toMillis()).pageCount(
                            (int) Math.ceil(lineCount/100)).build());

        } catch (IOException e) {
            throw new DocumentParseException("Failed to parse text file: " + fileName, e);
        } catch (Exception e) {
            throw new DocumentParseException("Unexpected error while parsing text file: " + fileName, e);
        }

        return result;
    }

    @Override
    public Set<String> supportedTypes() {
        return Set.of("txt");
    }
}