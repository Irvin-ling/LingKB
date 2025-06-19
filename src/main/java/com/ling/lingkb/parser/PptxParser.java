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

import com.alibaba.fastjson.JSON;
import com.ling.lingkb.common.entity.DocumentParseResult;
import com.ling.lingkb.common.exception.DocumentParseException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFNotes;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Component;

/**
 * PPT文档解析
 *
 * @author shipotian
 * @date 2025/6/19
 * @since 1.0.0
 */
@Component
public class PptxParser implements DocumentParser {
    /**
     * 每批次处理的最大幻灯片数
     */
    private static final int MAX_SLIDES_BATCH = 100;

    @Override
    public DocumentParseResult parse(File file) throws DocumentParseException {
        String fileName = file.getName();
        DocumentParseResult result = new DocumentParseResult();
        result.setSourceFileName(fileName);

        try (FileInputStream fis = new FileInputStream(file); XMLSlideShow ppt = new XMLSlideShow(fis)) {
            // 提取元数据（空值安全处理）
            POIXMLProperties props = ppt.getProperties();
            String author =
                    props.getCoreProperties().getCreator() != null ? props.getCoreProperties().getCreator() : "";
            String creationDate =
                    props.getCoreProperties().getCreated() != null ? props.getCoreProperties().getCreated().toString() :
                            "";
            int slideCount = ppt.getSlides().size();

            // 构建元数据
            result.setMetadata(DocumentParseResult.DocumentMetadata.builder().author(author).creationDate(creationDate)
                    .pageCount(String.valueOf(slideCount)).build());

            // 流式提取文本（分批次处理）
            String textContent = extractSlideText(ppt);
            result.setTextContent(textContent);

        } catch (IOException e) {
            throw new DocumentParseException("解析PPTX文件失败: " + fileName, e);
        }

        return result;
    }

    /**
     * 分批次提取幻灯片文本（大文件优化）
     */
    private String extractSlideText(XMLSlideShow ppt) {
        Map<Integer, List<String>> slideTextMap = new TreeMap<>();
        List<XSLFSlide> slides = ppt.getSlides();
        int slideIndex = 0;

        while (slideIndex < slides.size()) {
            int endIndex = Math.min(slideIndex + MAX_SLIDES_BATCH, slides.size());

            // 处理当前批次幻灯片
            for (int i = slideIndex; i < endIndex; i++) {
                XSLFSlide slide = slides.get(i);
                List<String> textList = new ArrayList<>();

                // 提取标题文本
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape) {
                        String text = ((XSLFTextShape) shape).getText();
                        if (text != null && !text.trim().isEmpty()) {
                            textList.add(text);
                        }
                    }
                }

                // 提取备注文本
                XSLFNotes notes = slide.getNotes();
                if (notes != null) {
                    for (XSLFShape noteShape : notes.getShapes()) {
                        if (noteShape instanceof XSLFTextShape) {
                            String noteText = ((XSLFTextShape) noteShape).getText();
                            if (noteText != null && !noteText.trim().isEmpty()) {
                                textList.add("备注: " + noteText);
                            }
                        }
                    }
                }
                slideTextMap.put(i + 1, textList);
            }

            slideIndex = endIndex;
        }

        return JSON.toJSONString(slideTextMap);
    }

    @Override
    public List<String> supportedTypes() {
        return List.of("pptx");
    }
}
