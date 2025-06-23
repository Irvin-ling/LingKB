package com.ling.lingkb.data.parser;

import com.ling.lingkb.common.entity.DocumentParseResult;
import com.ling.lingkb.common.exception.DocumentParseException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import javax.imageio.ImageIO;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Image Text Parser (OCR)
 * <p>
 * Extracts text from images using OCR technology.
 * </p>
 * <p>
 * <strong>Dependency Note:</strong> Download the English language package (<code>eng.traineddata</code>)
 * and Simplified Chinese package (<code>chi_sim.traineddata</code>) from the
 * <a href="https://github.com/tesseract-ocr/tessdata">Tesseract OCR repository</a>,
 * and place the packages in the specified Tesseract data path (<code>TESSDATA_PATH</code>).
 * </p>
 *
 * @author shipotian
 * @since 1.0.0
 */
@Component
public class ImageParser implements DocumentParser {

    @Value("${image.parser.tess.data.path:/root}")
    private String tessDataPath;

    private static final int MAX_TEXT_LENGTH = 10_000_000;

    private final Tesseract tesseract;

    public ImageParser() {
        this.tesseract = new Tesseract();
        this.tesseract.setDatapath(tessDataPath);
        this.tesseract.setLanguage("eng+chi_sim");
        this.tesseract.setPageSegMode(6);
        this.tesseract.setOcrEngineMode(1);
        this.tesseract.setHocr(false);
    }

    public void setTessDataPath(String tessDataPath) {
        this.tessDataPath = tessDataPath;
        this.tesseract.setDatapath(tessDataPath);
    }

    @Override
    public DocumentParseResult parse(Path filePath) throws DocumentParseException {
        String fileName = filePath.getFileName().toString();
        DocumentParseResult result = new DocumentParseResult();

        try {
            BufferedImage image = ImageIO.read(filePath.toFile());
            if (image == null) {
                throw new DocumentParseException("Unsupported or corrupted image file: " + fileName);
            }

            // OCR processing
            String extractedText = tesseract.doOCR(image);
            if (extractedText.length() > MAX_TEXT_LENGTH) {
                extractedText =
                        extractedText.substring(0, MAX_TEXT_LENGTH) + "\n\n[Content truncated due to size limit]";
            }

            result.setTextContent(extractedText);
            result.setMetadata(DocumentParseResult.DocumentMetadata.builder().sourceFileName(fileName)
                    .creationDate(Files.getLastModifiedTime(filePath).toMillis()).pageCount(1).build());

        } catch (IOException e) {
            throw new DocumentParseException("Failed to read image file: " + fileName, e);
        } catch (TesseractException e) {
            throw new DocumentParseException("OCR processing failed for file: " + fileName, e);
        } catch (Exception e) {
            throw new DocumentParseException("Unexpected error processing image file: " + fileName, e);
        }

        return result;
    }

    @Override
    public Set<String> supportedTypes() {
        return Set.of("png", "jpg", "jpeg", "tiff", "bmp", "gif");
    }

    /**
     * Configures OCR settings for better accuracy
     */
    public void configureOcr(String language, String tessDataPath) {
        this.tesseract.setLanguage(language);
        this.tesseract.setDatapath(tessDataPath);
    }
}