package com.ling.lingkb.data.parser;

import com.ling.lingkb.common.entity.DocumentParseResult;
import com.ling.lingkb.common.exception.DocumentParseException;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Text Document Parser
 * <p>
 * Parses plain text files (TXT) and extracts content with metadata
 * </p>
 *
 * @author shipotian
 * @since 1.0.0
 */
@Component
public class TxtParser implements DocumentParser {
    private static final int MAX_LINES = 100_000;
    private static final int MAX_TEXT_LENGTH = 10_000_000;

    @Override
    public DocumentParseResult parse(Path filePath) throws DocumentParseException {
        String fileName = filePath.getFileName().toString();
        DocumentParseResult result = new DocumentParseResult();

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            StringBuilder textBuilder = new StringBuilder();
            String line;
            int lineCount = 0;
            long charCount = 0;

            while ((line = reader.readLine()) != null && lineCount < MAX_LINES) {
                if (charCount + line.length() > MAX_TEXT_LENGTH) {
                    textBuilder.append("...[content truncated due to size limit]");
                    break;
                }

                textBuilder.append(line).append("\n");
                lineCount++;
                charCount += line.length();
            }

            result.setTextContent(textBuilder.toString());
            result.setMetadata(DocumentParseResult.DocumentMetadata.builder().author("Unknown").sourceFileName(fileName)
                    .creationDate(Files.getLastModifiedTime(filePath).toMillis()).pageCount(lineCount).build());

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