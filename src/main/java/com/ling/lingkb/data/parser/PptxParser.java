package com.ling.lingkb.data.parser;

import com.ling.lingkb.entity.DocumentParseResult;
import com.ling.lingkb.exception.DocumentParseException;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFNotes;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * PowerPoint Document Parser
 * <p>
 * Parses PPTX files and extracts content with metadata
 * </p>
 *
 * @author shipotian
 * @version 1.0.0
 */
@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "data.parser.pptx")
public class PptxParser implements DocumentParser {
    private int maxSlidesBatch = 100;
    private int maxTextLength = 10_000_000;

    @Override
    public DocumentParseResult parse(Path filePath) throws DocumentParseException {
        log.info("PptxParser.parse({})...", filePath);
        String fileName = filePath.getFileName().toString();
        DocumentParseResult result = new DocumentParseResult();

        try (FileInputStream fis = new FileInputStream(filePath.toFile()); XMLSlideShow ppt = new XMLSlideShow(fis)) {

            // Extract metadata
            POIXMLProperties props = ppt.getProperties();
            String author =
                    props.getCoreProperties().getCreator() != null ? props.getCoreProperties().getCreator() : "Unknown";
            Date creationDate = props.getCoreProperties().getCreated();
            int slideCount = ppt.getSlides().size();

            // Set metadata
            result.setMetadata(DocumentParseResult.DocumentMetadata.builder().author(author).sourceFileName(fileName)
                    .creationDate(creationDate != null ? creationDate.getTime() : 0).pageCount(slideCount).build());

            // Extract text content
            String textContent = extractSlideText(ppt);
            if (textContent.length() > maxTextLength) {
                log.warn("current file content truncated due to size limit {}", maxTextLength);
                textContent = textContent.substring(0, maxTextLength) + "...[truncated]";
            }
            result.setText(textContent);

        } catch (IOException e) {
            throw new DocumentParseException("Failed to parse PPTX file: " + fileName, e);
        } catch (Exception e) {
            throw new DocumentParseException("Unexpected error while parsing PPTX file: " + fileName, e);
        }

        return result;
    }

    /**
     * Extracts text from slides with batch processing
     */
    private String extractSlideText(XMLSlideShow ppt) {
        StringBuilder contentBuilder = new StringBuilder();
        List<XSLFSlide> slides = ppt.getSlides();
        int slideIndex = 0;

        while (slideIndex < slides.size()) {
            int endIndex = Math.min(slideIndex + maxSlidesBatch, slides.size());

            for (int i = slideIndex; i < endIndex; i++) {
                XSLFSlide slide = slides.get(i);
                contentBuilder.append("Slide ").append(i + 1).append(":\n");

                // Extract slide text
                String slideText = extractShapesText(slide.getShapes());
                contentBuilder.append(slideText);

                // Extract notes text
                XSLFNotes notes = slide.getNotes();
                if (notes != null) {
                    String notesText = extractShapesText(notes.getShapes());
                    if (!notesText.isEmpty()) {
                        contentBuilder.append("Notes: ").append(notesText).append("\n");
                    }
                }

                contentBuilder.append("\n");

                // Check size limit
                if (contentBuilder.length() > maxTextLength) {
                    return contentBuilder.toString();
                }
            }
            slideIndex = endIndex;
        }

        return contentBuilder.toString();
    }

    /**
     * Extracts text from shapes
     */
    private String extractShapesText(List<XSLFShape> shapes) {
        return shapes.stream().filter(shape -> shape instanceof XSLFTextShape)
                .map(shape -> ((XSLFTextShape) shape).getText()).filter(text -> text != null && !text.trim().isEmpty())
                .collect(Collectors.joining("\n"));
    }

    @Override
    public Set<String> supportedTypes() {
        return Set.of("pptx");
    }
}