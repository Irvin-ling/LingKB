package com.ling.lingkb.llm.data.parser;

import com.ling.lingkb.entity.LingDocument;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFNotes;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.beans.factory.annotation.Value;
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
@Slf4j
@Component
public class PptxParser implements DocumentParser {
    @Value("${data.parser.max.length}")
    private int dataMaxLength;

    @Override
    public LingDocument parse(Path filePath) throws Exception {
        log.info("PptxParser.parse({})...", filePath);
        String fileName = filePath.getFileName().toString();
        LingDocument result = new LingDocument();

        try (FileInputStream fis = new FileInputStream(filePath.toFile()); XMLSlideShow ppt = new XMLSlideShow(fis)) {
            // Extract metadata
            POIXMLProperties props = ppt.getProperties();
            String author =
                    props.getCoreProperties().getCreator() != null ? props.getCoreProperties().getCreator() : "Unknown";
            Date creationDate = props.getCoreProperties().getCreated();
            int slideCount = ppt.getSlides().size();
            result.setAuthor(author);
            result.setSourceFileName(fileName);
            result.setCreationDate(creationDate != null ? creationDate.getTime() : 0);
            result.setPageCount(slideCount);
            // Extract text content
            String textContent = extractSlideText(ppt);
            if (textContent.length() > dataMaxLength) {
                log.warn("current file-{} content truncated due to size limit {}", fileName, dataMaxLength);
                textContent = textContent.substring(0, dataMaxLength);
            }
            result.setText(textContent);
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
            int endIndex = Math.min(slideIndex + 10, slides.size());

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
                if (contentBuilder.length() > dataMaxLength) {
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
        return Set.of("pptx", "ppt");
    }
}