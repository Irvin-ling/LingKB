package com.ling.lingkb.llm.data.parser;

import com.ling.lingkb.entity.LingDocument;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
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
 * @version 1.0.0
 */
@Slf4j
@Component
public class ImageParser implements DocumentParser {
    @Value("${data.parser.image.switch}")
    private boolean imageSwitch;
    @Value("${data.parser.max.length}")
    private int dataMaxLength;
    @Value("${data.parser.tess.path}")
    private String dataTessPath;

    private final Tesseract tesseract = new Tesseract();

    @PostConstruct
    public void init() {
        if (imageSwitch) {
            this.tesseract.setDatapath(dataTessPath);
            this.tesseract.setLanguage("chi_sim");
            this.tesseract.setPageSegMode(6);
            this.tesseract.setOcrEngineMode(1);
            this.tesseract.setHocr(false);
        }
    }

    @Override
    public LingDocument parse(Path filePath) throws Exception {
        log.info("ImageParser.parse({})...", filePath);
        String fileName = filePath.getFileName().toString();
        if (imageSwitch) {
            LingDocument result = new LingDocument();

            BufferedImage image = ImageIO.read(filePath.toFile());
            if (image == null) {
                throw new Exception("Unsupported or corrupted image file: " + fileName);
            }

            result.setSourceFileName(fileName);
            result.setCreationDate(Files.getLastModifiedTime(filePath).toMillis());
            result.setPageCount(1);
            // OCR processing
            String extractedText = tesseract.doOCR(image);
            if (extractedText.length() > dataMaxLength) {
                log.warn("current file-{} content truncated due to size limit {}", fileName, dataMaxLength);
                extractedText = extractedText.substring(0, dataMaxLength);
            }
            result.setText(extractedText);
            return result;
        }
        log.error("Unsupported file-{} format", fileName);
        throw new Exception("Unsupported file format");
    }

    @Override
    public Set<String> supportedTypes() {
        return Set.of("png", "jpg", "jpeg", "tiff", "bmp", "gif");
    }

}