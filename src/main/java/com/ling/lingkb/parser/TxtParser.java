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

import com.ling.lingkb.common.entity.DocumentParseResult;
import com.ling.lingkb.common.exception.DocumentParseException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Txt解析器
 *
 * @author shipotian
 * @date 2025/6/19
 * @since 1.0.0
 */
@Component
public class TxtParser implements DocumentParser {
    /**
     * 最大读取行数
     */
    private static final int MAX_LINES = 100_000;

    @Override
    public DocumentParseResult parse(File file) throws DocumentParseException {
        String fileName = file.getName();
        DocumentParseResult result = new DocumentParseResult();
        result.setSourceFileName(fileName);

        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            StringBuilder textBuilder = new StringBuilder();
            String line;
            int lineCount = 0;

            // 流式读取文本内容
            while ((line = reader.readLine()) != null && lineCount < MAX_LINES) {
                textBuilder.append(line).append("\n");
                lineCount++;
            }

            result.setTextContent(textBuilder.toString());

            // 提取元数据
            result.setMetadata(
                    DocumentParseResult.DocumentMetadata.builder().creationDate(String.valueOf(file.lastModified()))
                            .pageCount(String.valueOf(lineCount)) // 使用行数作为“页数”
                            .build());

            return result;
        } catch (IOException e) {
            throw new DocumentParseException("解析TXT文件失败: " + fileName, e);
        }
    }

    @Override
    public List<String> supportedTypes() {
        return List.of("txt");
    }
}
