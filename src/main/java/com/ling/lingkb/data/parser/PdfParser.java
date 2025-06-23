package com.ling.lingkb.data.parser;

import com.ling.lingkb.common.entity.DocumentParseResult;
import com.ling.lingkb.common.exception.DocumentParseException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
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
 * @since 1.0.0
 */
@Component
public class PdfParser implements DocumentParser {
    private static final int MAX_PAGE_BATCH = 100;
    private static final int MAX_TEXT_LENGTH = 10_000_000;

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
                int endPage = Math.min(startPage + MAX_PAGE_BATCH - 1, totalPages);
                stripper.setStartPage(startPage);
                stripper.setEndPage(endPage);

                String batchText = stripper.getText(document);
                if (textBuilder.length() + batchText.length() > MAX_TEXT_LENGTH) {
                    textBuilder.append("...[content truncated due to size limit]");
                    break;
                }
                textBuilder.append(batchText);
                startPage = endPage + 1;
            }

            result.setTextContent(textBuilder.toString());
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