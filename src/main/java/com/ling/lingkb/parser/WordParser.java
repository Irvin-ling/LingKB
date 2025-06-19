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
import com.ling.lingkb.common.exception.UnsupportedDocumentTypeException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.poi.extractor.POITextExtractor;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

/**
 * Word文件解析
 *
 * @author shipotian
 * @date 2025/6/19
 * @since 1.0.0
 */
@Component
public class WordParser implements DocumentParser {
    /**
     * 50MB 文本截断阈值
     */
    private static final int MAX_TEXT_SIZE = 50 * 1024 * 1024;

    @Override
    public DocumentParseResult parse(File file) throws DocumentParseException {
        String fileName = file.getName();
        DocumentParseResult result = new DocumentParseResult();
        result.setSourceFileName(fileName);

        try {
            if (fileName.endsWith(".docx")) {
                // 处理 .docx 文件（OOXML 格式）
                try (FileInputStream fis = new FileInputStream(file); XWPFDocument doc = new XWPFDocument(fis)) {

                    // 提取文本内容（使用流式读取优化）
                    String text = extractDocxText(doc);
                    result.setTextContent(text);

                    // 提取元数据（空值安全处理）
                    String author = doc.getProperties().getCoreProperties().getCreator();
                    String creationDate = doc.getProperties().getCoreProperties().getCreated() != null ?
                            doc.getProperties().getCoreProperties().getCreated().toString() : "";
                    int pageCount = doc.getProperties().getExtendedProperties().getPages();

                    result.setMetadata(
                            DocumentParseResult.DocumentMetadata.builder().author(author != null ? author : "")
                                    .creationDate(creationDate).pageCount(String.valueOf(pageCount)).build());
                }
            } else if (fileName.endsWith(".doc")) {
                // 处理 .doc 文件（二进制格式）
                try (FileInputStream fis = new FileInputStream(file); HWPFDocument doc = new HWPFDocument(fis)) {

                    // 提取文本内容（使用流式读取优化）
                    String text = extractDocText(doc);
                    result.setTextContent(text);

                    // 提取元数据（空值安全处理）
                    String author = doc.getSummaryInformation().getAuthor();
                    String creationDate = doc.getSummaryInformation().getCreateDateTime() != null ?
                            doc.getSummaryInformation().getCreateDateTime().toString() : "";
                    int pageCount = doc.getSummaryInformation().getPageCount();

                    result.setMetadata(
                            DocumentParseResult.DocumentMetadata.builder().author(author != null ? author : "")
                                    .creationDate(creationDate).pageCount(String.valueOf(pageCount)).build());
                }
            } else {
                throw new UnsupportedDocumentTypeException("不支持的Word文件格式: " + fileName);
            }

            return result;
        } catch (IOException e) {
            throw new DocumentParseException("解析Word失败：" + fileName, e);
        }
    }

    /**
     * 提取 .docx 文件的文本内容（支持大文件，带截断功能）
     */
    private String extractDocxText(XWPFDocument doc) throws IOException {
        XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
        return extractText(extractor);
    }

    /**
     * 提取 .doc 文件的文本内容（支持大文件，带截断功能）
     */
    private String extractDocText(HWPFDocument doc) throws IOException {
        WordExtractor extractor = new WordExtractor(doc);
        return extractText(extractor);
    }

    private String extractText(POITextExtractor extractor) throws IOException {
        StringBuilder textBuilder = new StringBuilder();
        try {
            // 提取主体文本
            String bodyText = extractor.getText();
            textBuilder.append(bodyText);

            // 检查是否超过最大长度
            if (textBuilder.length() > MAX_TEXT_SIZE) {
                textBuilder.setLength(MAX_TEXT_SIZE);
                textBuilder.append("\n\n[文档过大，已截断前50MB内容]");
            }

            return textBuilder.toString();
        } finally {
            extractor.close(); // 确保资源释放
        }
    }


    @Override
    public List<String> supportedTypes() {
        return Arrays.asList("doc", "docx");
    }
}
