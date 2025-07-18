package com.ling.lingkb.llm.data.parser;

import com.ling.lingkb.entity.LingDocument;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
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
@Slf4j
@Component
public class WordParser implements DocumentParser {
    @Value("${data.parser.max.length}")
    private int dataMaxLength;

    @Override
    public LingDocument parse(Path filePath) throws Exception {
        log.info("WordParser.parse({})...", filePath);
        String fileName = filePath.getFileName().toString();
        LingDocument result = new LingDocument();
        if (fileName.toLowerCase().endsWith(".docx")) {
            return parseDocx(filePath, result);
        } else if (fileName.toLowerCase().endsWith(".doc")) {
            return parseDoc(filePath, result);
        }
        throw new Exception("Incorrect file suffix:" + fileName);
    }

    private LingDocument parseDocx(Path filePath, LingDocument result) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath.toFile()); XWPFDocument doc = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            String author = doc.getProperties().getCoreProperties().getCreator();
            Date created = doc.getProperties().getCoreProperties().getCreated();
            int pageCount = doc.getProperties().getExtendedProperties().getPages();
            return getLingDocument(filePath, result, author, created, pageCount, extractor.getText());
        }
    }

    private LingDocument parseDoc(Path filePath, LingDocument result) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath.toFile()); HWPFDocument doc = new HWPFDocument(fis);
             WordExtractor extractor = new WordExtractor(doc)) {
            String author = doc.getSummaryInformation().getAuthor();
            Date created = doc.getSummaryInformation().getCreateDateTime();
            int pageCount = doc.getSummaryInformation().getPageCount();
            return getLingDocument(filePath, result, author, created, pageCount, extractor.getText());
        }
    }

    private LingDocument getLingDocument(Path filePath, LingDocument result, String author, Date created, int pageCount,
                                         String text) {
        result.setAuthor(author != null ? author : "Unknown");
        result.setSourceFileName(filePath.getFileName().toString());
        result.setCreationDate(created != null ? created.getTime() : 0);
        result.setPageCount(pageCount);
        String content = truncateText(result.getSourceFileName(), text);
        result.setText(content);
        return result;
    }

    private String truncateText(String fileName, String text) {
        if (text == null) {
            return "";
        }
        if (text.length() > dataMaxLength) {
            log.warn("current file-{} content truncated due to size limit {}", fileName, dataMaxLength);
            return text.substring(0, dataMaxLength);
        }
        return text;
    }

    @Override
    public Set<String> supportedTypes() {
        return Set.of("doc", "docx");
    }
}