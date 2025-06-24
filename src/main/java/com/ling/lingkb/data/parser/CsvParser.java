package com.ling.lingkb.data.parser;

import com.alibaba.fastjson.JSON;
import com.ling.lingkb.common.entity.DocumentParseResult;
import com.ling.lingkb.common.exception.DocumentParseException;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author shipotian
 * @date 2025/6/19
 * @since 1.0.0
 */
@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "data.parser.csv")
public class CsvParser implements DocumentParser {
    private int defaultBatchSize = 1000;
    private int maxRows = 100_000;
    private int maxColumns = 100;

    @Override
    public DocumentParseResult parse(Path filePath) throws DocumentParseException {
        String fileName = filePath.getFileName().toString();
        DocumentParseResult result = new DocumentParseResult();
        AtomicLong rowCounter = new AtomicLong(0);
        try {
            result.setMetadata(DocumentParseResult.DocumentMetadata.builder().author("Unknown").sourceFileName(fileName)
                    .creationDate(Files.getLastModifiedTime(filePath).toMillis()).pageCount(1).build());

            // Configure robust CSV format
            CSVFormat format =
                    CSVFormat.DEFAULT.builder().setSkipHeaderRecord(false).setTrim(true).setIgnoreEmptyLines(true)
                            .setIgnoreSurroundingSpaces(true).setRecordSeparator('\n').build();
            // Using streaming processing to process data in batches
            try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
                 CSVParser csvParser = new CSVParser(reader, format)) {
                List<List<String>> batch = new ArrayList<>(defaultBatchSize);
                for (CSVRecord record : csvParser) {
                    if (rowCounter.incrementAndGet() > maxRows) {
                        log.warn("current file content truncated due to size limit {} rows", maxRows);
                        break;
                    }
                    List<String> row = convertToRow(record);
                    batch.add(row);
                    if (batch.size() >= defaultBatchSize) {
                        processBatch(batch, result);
                        batch.clear();
                    }
                }
                if (!batch.isEmpty()) {
                    processBatch(batch, result);
                }
            }
        } catch (IOException e) {
            throw new DocumentParseException("Failed to parse CSV file: " + fileName, e);
        }

        return result;
    }

    private List<String> convertToRow(CSVRecord record) {
        List<String> row = new ArrayList<>();
        for (int i = 0; i < record.size() && i < maxColumns; i++) {
            row.add(record.get(i));
        }
        return row;
    }

    private void processBatch(List<List<String>> batch, DocumentParseResult result) {
        String jsonBatch = JSON.toJSONString(batch);
        if (result.getTextContent() == null) {
            result.setTextContent(jsonBatch);
        } else {
            result.setTextContent(result.getTextContent().substring(0, result.getTextContent().length() - 1) + "," +
                    jsonBatch.substring(1));
        }
    }

    @Override
    public Set<String> supportedTypes() {
        return Set.of("csv", "tsv");
    }
}