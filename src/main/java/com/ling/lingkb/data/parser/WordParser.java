package com.ling.lingkb.data.parser;

import com.ling.lingkb.entity.DocumentParseResult;
import com.ling.lingkb.exception.DocumentParseException;
import com.ling.lingkb.exception.UnsupportedDocumentTypeException;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.Set;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Word Document Parser
 * <p>
 * Parses Word files (DOC/DOCX) and extracts content with metadata
 * </p>
 *
 * @author shipotian
 * @version 1.0.0
 */
@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "data.parser.word")
public class WordParser implements DocumentParser {
    private int maxTextSize = 10_000_000;
    private static final String TRUNCATION_NOTICE = "\n\n[Document truncated to first 50MB]";

    @Override
    public DocumentParseResult parse(Path filePath) throws DocumentParseException {
        String fileName = filePath.getFileName().toString();
        DocumentParseResult result = new DocumentParseResult();

        try {
            if (fileName.toLowerCase().endsWith(".docx")) {
                return parseDocx(filePath, result);
            } else if (fileName.toLowerCase().endsWith(".doc")) {
                return parseDoc(filePath, result);
            }
            throw new UnsupportedDocumentTypeException("Unsupported Word file format: " + fileName);
        } catch (IOException e) {
            throw new DocumentParseException("Failed to parse Word file: " + fileName, e);
        } catch (Exception e) {
            throw new DocumentParseException("Unexpected error while parsing Word file: " + fileName, e);
        }
    }

    private DocumentParseResult parseDocx(Path filePath, DocumentParseResult result) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath.toFile()); XWPFDocument doc = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {

            String text = truncateText(extractor.getText());
            result.setText(text);

            // Set metadata
            String author = doc.getProperties().getCoreProperties().getCreator();
            Date created = doc.getProperties().getCoreProperties().getCreated();
            int pageCount = doc.getProperties().getExtendedProperties().getPages();

            result.setMetadata(
                    DocumentParseResult.DocumentMetadata.builder().author(author != null ? author : "Unknown")
                            .sourceFileName(filePath.getFileName().toString())
                            .creationDate(created != null ? created.getTime() : 0).pageCount(pageCount).build());

            return result;
        }
    }

    private DocumentParseResult parseDoc(Path filePath, DocumentParseResult result) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath.toFile()); HWPFDocument doc = new HWPFDocument(fis);
             WordExtractor extractor = new WordExtractor(doc)) {

            String text = truncateText(extractor.getText());
            result.setText(text);

            // Set metadata
            String author = doc.getSummaryInformation().getAuthor();
            Date created = doc.getSummaryInformation().getCreateDateTime();
            int pageCount = doc.getSummaryInformation().getPageCount();

            result.setMetadata(
                    DocumentParseResult.DocumentMetadata.builder().author(author != null ? author : "Unknown")
                            .sourceFileName(filePath.getFileName().toString())
                            .creationDate(created != null ? created.getTime() : 0).pageCount(pageCount).build());

            return result;
        }
    }

    private String truncateText(String text) {
        if (text == null) {
            return "";
        }
        if (text.length() > maxTextSize) {
            log.warn("current file content truncated due to size limit {}", maxTextSize);
            return text.substring(0, maxTextSize) + TRUNCATION_NOTICE;
        }
        return text;
    }

    @Override
    public Set<String> supportedTypes() {
        return Set.of("doc", "docx");
    }
}