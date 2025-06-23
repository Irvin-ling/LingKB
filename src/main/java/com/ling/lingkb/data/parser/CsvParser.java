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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

/**
 * @author shipotian
 * @date 2025/6/19
 * @since 1.0.0
 */
@Slf4j
@Component
public class CsvParser implements DocumentParser {
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final int MAX_ROWS = 100_000;
    private static final int MAX_COLUMNS = 100;

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
                List<List<String>> batch = new ArrayList<>(DEFAULT_BATCH_SIZE);
                for (CSVRecord record : csvParser) {
                    if (rowCounter.incrementAndGet() > MAX_ROWS) {
                        log.warn("CSV file exceeds the maximum line limit {}, processing has been stopped", MAX_ROWS);
                        break;
                    }
                    List<String> row = convertToRow(record);
                    batch.add(row);
                    if (batch.size() >= DEFAULT_BATCH_SIZE) {
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
        for (int i = 0; i < record.size() && i < MAX_COLUMNS; i++) {
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