package com.ling.lingkb.data.parser;

import com.ling.lingkb.entity.DocumentParseResult;
import com.ling.lingkb.exception.DocumentParseException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.Set;

/**
 * PDF Document Parser
 * <p>
 * Parses PDF files and extracts content with metadata
 * </p>
 *
 * @author shipotian
 * @version 1.0.0
 */
@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "data.parser.pdf")
public class PdfParser implements DocumentParser {
    private int maxPageBatch = 100;
    private int maxTextLength = 10_000_000;

    @Override
    public DocumentParseResult parse(Path filePath) throws DocumentParseException {
        String fileName = filePath.getFileName().toString();
        DocumentParseResult result = new DocumentParseResult();

        try (PDDocument document = Loader.loadPDF(filePath.toFile())) {
            // Extract metadata with null checks
            PDDocumentInformation info = document.getDocumentInformation();
            String author = info != null && info.getAuthor() != null ? info.getAuthor() : "Unknown";
            Date creationDate = info != null && info.getCreationDate() != null ? info.getCreationDate().getTime() : null;
            int totalPages = document.getNumberOfPages();

            // Set metadata
            result.setMetadata(DocumentParseResult.DocumentMetadata.builder()
                    .author(author)
                    .sourceFileName(fileName)
                    .creationDate(creationDate != null ? creationDate.getTime() : 0)
                    .pageCount(totalPages)
                    .build());

            // Extract text content with batch processing
            StringBuilder textBuilder = new StringBuilder();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            int startPage = 1;
            while (startPage <= totalPages) {
                int endPage = Math.min(startPage + maxPageBatch - 1, totalPages);
                stripper.setStartPage(startPage);
                stripper.setEndPage(endPage);

                String batchText = stripper.getText(document);
                if (textBuilder.length() + batchText.length() > maxTextLength) {
                    textBuilder.append("...[content truncated due to size limit]");
                    log.warn("current file content truncated due to size limit {}", maxTextLength);
                    break;
                }
                textBuilder.append(batchText);
                startPage = endPage + 1;
            }

            result.setText(textBuilder.toString());
        } catch (IOException e) {
            throw new DocumentParseException("Failed to parse PDF file: " + fileName, e);
        } catch (Exception e) {
            throw new DocumentParseException("Unexpected error while parsing PDF file: " + fileName, e);
        }

        return result;
    }

    @Override
    public Set<String> supportedTypes() {
        return Set.of("pdf");
    }
}