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
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/**
 * PDF文件解析
 *
 * @author shipotian
 * @date 2025/6/19
 * @since 1.0.0
 */
@Component
public class PdfParser implements DocumentParser {
    /**
     * 每批次处理的最大页数
     */
    private static final int MAX_PAGE_BATCH = 100;

    @Override
    public DocumentParseResult parse(File file) throws DocumentParseException {
        String fileName = file.getName();
        DocumentParseResult result = new DocumentParseResult();
        result.setSourceFileName(fileName);

        try (PDDocument document = Loader.loadPDF(file)) {
            // 提取元数据（空值安全处理）
            PDDocumentInformation info = document.getDocumentInformation();
            String author = info.getAuthor() != null ? info.getAuthor() : "";
            String creationDate = info.getCreationDate() != null ? info.getCreationDate().toString() : "";
            int totalPages = document.getNumberOfPages();

            // 构建元数据
            result.setMetadata(DocumentParseResult.DocumentMetadata.builder().author(author).creationDate(creationDate)
                    .pageCount(String.valueOf(totalPages)).build());

            // 流式提取文本（分批次处理）
            StringBuilder textBuilder = new StringBuilder();
            PDFTextStripper stripper = new PDFTextStripper();
            // 按位置排序文本，适配复杂布局
            stripper.setSortByPosition(true);

            int startPage = 1;
            int endPage = Math.min(MAX_PAGE_BATCH, totalPages);

            while (startPage <= totalPages) {
                // 设置当前批次页码范围
                stripper.setStartPage(startPage);
                stripper.setEndPage(endPage);

                // 提取当前批次文本
                String batchText = stripper.getText(document);
                textBuilder.append(batchText);

                // 移动到下一批次
                startPage = endPage + 1;
                endPage = Math.min(startPage + MAX_PAGE_BATCH - 1, totalPages);
            }

            result.setTextContent(textBuilder.toString());
            return result;
        } catch (IOException e) {
            throw new DocumentParseException("解析PDF失败：" + fileName, e);
        }
    }

    @Override
    public List<String> supportedTypes() {
        return Arrays.asList("pdf", "PDF");
    }
}
