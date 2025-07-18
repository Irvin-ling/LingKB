package com.ling.lingkb.llm.data.parser;

import com.alibaba.fastjson.JSON;
import com.ling.lingkb.entity.LingDocument;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/19
 */
@Slf4j
@Component
public class CsvParser implements DocumentParser {
    @Value("${data.parser.max.row}")
    private int dataMaxRow;

    @Override
    public LingDocument parse(Path filePath) throws Exception {
        log.info("CsvParser.parse({})...", filePath);
        String fileName = filePath.getFileName().toString();
        LingDocument result = new LingDocument();
        result.setAuthor("Unknown");
        result.setSourceFileName(fileName);
        result.setCreationDate(Files.getLastModifiedTime(filePath).toMillis());
        result.setPageCount(1);
        // Configure robust CSV format
        CSVFormat format =
                CSVFormat.DEFAULT.builder().setSkipHeaderRecord(false).setTrim(true).setIgnoreEmptyLines(true)
                        .setIgnoreSurroundingSpaces(true).setRecordSeparator('\n').build();
        // Using streaming processing to process data in batches
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
             CSVParser csvParser = new CSVParser(reader, format)) {
            List<List<String>> contentList = new ArrayList<>();
            for (CSVRecord record : csvParser) {
                List<String> row = convertToRow(record);
                contentList.add(row);
                if (contentList.size() >= dataMaxRow) {
                    break;
                }
            }
            result.setText(JSON.toJSONString(contentList));
        }
        return result;
    }

    private List<String> convertToRow(CSVRecord record) {
        List<String> row = new ArrayList<>();
        for (int i = 0; i < record.size(); i++) {
            row.add(record.get(i));
        }
        return row;
    }

    @Override
    public Set<String> supportedTypes() {
        return Set.of("csv", "tsv");
    }
}