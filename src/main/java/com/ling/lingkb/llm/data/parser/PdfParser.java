package com.ling.lingkb.llm.data.parser;

import com.ling.lingkb.entity.LingDocument;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * PDF Document Parser
 * <p>
 * Parses PDF files and extracts content with metadata
 * </p>
 *
 * @author shipotian
 * @version 1.0.0
 */
@Slf4j
@Component
public class PdfParser implements DocumentParser {
    @Value("${data.parser.max.length}")
    private int dataMaxLength;

    @Override
    public LingDocument parse(Path filePath) throws Exception {
        log.info("PdfParser.parse({})...", filePath);
        String fileName = filePath.getFileName().toString();
        LingDocument result = new LingDocument();

        try (PDDocument document = Loader.loadPDF(filePath.toFile())) {
            // Extract metadata with null checks
            PDDocumentInformation info = document.getDocumentInformation();
            String author = info != null && info.getAuthor() != null ? info.getAuthor() : "Unknown";
            Date creationDate =
                    info != null && info.getCreationDate() != null ? info.getCreationDate().getTime() : null;
            int totalPages = document.getNumberOfPages();
            result.setAuthor(author);
            result.setSourceFileName(fileName);
            result.setCreationDate(creationDate != null ? creationDate.getTime() : 0);
            result.setPageCount(totalPages);
            // Extract text content with batch processing
            StringBuilder textBuilder = new StringBuilder();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            int startPage = 1;
            while (startPage <= totalPages) {
                int endPage = Math.min(startPage + 9, totalPages);
                stripper.setStartPage(startPage);
                stripper.setEndPage(endPage);

                String batchText = stripper.getText(document);
                textBuilder.append(batchText);
                if (textBuilder.length() > dataMaxLength) {
                    log.warn("current file content truncated due to size limit {}", dataMaxLength);
                    break;
                }
                startPage = endPage + 1;
            }
            result.setText(textBuilder.toString());
        }

        return result;
    }

    @Override
    public Set<String> supportedTypes() {
        return Collections.singleton("pdf");
    }
}