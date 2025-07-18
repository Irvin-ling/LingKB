package com.ling.lingkb.llm.data.parser;

import com.ling.lingkb.entity.LingDocument;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
@Slf4j
@Component
public class TxtParser implements DocumentParser {

    @Value("${data.parser.max.length}")
    private int dataMaxLength;

    @Override
    public LingDocument parse(Path filePath) throws Exception {
        log.info("TxtParser.parse({})...", filePath);
        String fileName = filePath.getFileName().toString();
        LingDocument result = new LingDocument();
        result.setAuthor("Unknown");
        result.setSourceFileName(fileName);
        result.setCreationDate(Files.getLastModifiedTime(filePath).toMillis());
        result.setPageCount(1);

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            StringBuilder textBuilder = new StringBuilder();
            String line;
            long charCount = 0;

            while ((line = reader.readLine()) != null) {
                if (charCount + line.length() > dataMaxLength) {
                    log.warn("current file content truncated due to size limit {}", dataMaxLength);
                    break;
                }

                textBuilder.append(line).append("\n");
                charCount += line.length();
            }
            result.setText(textBuilder.toString());
        }

        return result;
    }

    @Override
    public Set<String> supportedTypes() {
        return Set.of("txt");
    }
}