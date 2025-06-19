package com.ling.lingkb.parser;
/*
 * ------------------------------------------------------------------
 * Copyright @ 2025 Hangzhou Ling Technology Co.,Ltd. All rights reserved.
 * ------------------------------------------------------------------
 * Product: LingKB
 * Module Name: LingKB
 * Date Created: 2025/6/19
 * Description:
 * ------------------------------------------------------------------
 * Modification History
 * DATE            Name           Description
 * ------------------------------------------------------------------
 * 2025/6/19       spt
 * ------------------------------------------------------------------
 */

import com.alibaba.fastjson.JSON;
import com.ling.lingkb.common.entity.DocumentParseResult;
import com.ling.lingkb.common.exception.DocumentParseException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

/**
 * Csv文档解析器
 *
 * @author shipotian
 * @date 2025/6/19
 * @since 1.0.0
 */
@Component
public class CsvParser implements DocumentParser {
    private static final char DEFAULT_DELIMITER = ',';

    private List<String> parseCsvRow(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == CsvParser.DEFAULT_DELIMITER && !inQuotes) {
                cells.add(cell.toString());
                cell.setLength(0);
            } else {
                cell.append(c);
            }
        }
        cells.add(cell.toString());
        return cells;
    }

    @Override
    public DocumentParseResult parse(File file) throws DocumentParseException {
        String fileName = file.getName();
        DocumentParseResult result = new DocumentParseResult();
        result.setSourceFileName(fileName);

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            long pageCount = 0;
            File tempJsonFile = File.createTempFile("csv-parse-", ".json");
            try (FileWriter writer = new FileWriter(tempJsonFile)) {
                writer.write("[");
                String line;
                boolean isFirstRow = true;

                while ((line = reader.readLine()) != null) {
                    List<String> row = parseCsvRow(line);
                    if (!isFirstRow) {
                        writer.write(",");
                    }
                    writer.write(JSON.toJSONString(row));
                    isFirstRow = false;
                    pageCount++;
                }
                writer.write("]");
            }
            result.setTextContent(FileUtils.readFileToString(tempJsonFile, StandardCharsets.UTF_8));
            DocumentParseResult.DocumentMetadata documentMetadata =
                    DocumentParseResult.DocumentMetadata.builder().creationDate(String.valueOf(file.lastModified()))
                            .pageCount(String.valueOf(pageCount)).build();
            result.setMetadata(documentMetadata);
            tempJsonFile.deleteOnExit();
        } catch (IOException e) {
            throw new DocumentParseException("解析CSV文件失败: " + fileName, e);
        }

        return result;
    }

    @Override
    public List<String> supportedTypes() {
        return List.of("csv");
    }
}
