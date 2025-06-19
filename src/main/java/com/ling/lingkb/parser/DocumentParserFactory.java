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

import com.ling.lingkb.common.entity.DocumentParseResult;
import com.ling.lingkb.common.exception.DocumentParseException;
import com.ling.lingkb.common.exception.UnsupportedDocumentTypeException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.apache.tika.mime.MimeTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author shipotian
 * @date 2025/6/19
 * @since 1.0.0
 */
@Slf4j
@Component
public class DocumentParserFactory {
    private final List<DocumentParser> parsers;

    @Autowired
    public DocumentParserFactory(List<DocumentParser> parsers) {
        this.parsers = parsers;
    }

    /**
     * 根据文件扩展名获取解析器
     */
    private DocumentParser getParserByExtension(String extension) throws UnsupportedDocumentTypeException {
        return parsers.stream().filter(p -> p.supportedTypes().contains(extension.toLowerCase())).findFirst()
                .orElseThrow(() -> new UnsupportedDocumentTypeException("不支持的文档类型：" + extension));
    }

    /**
     * 自动检测类型并解析（使用Tika）
     */
    public DocumentParseResult autoParse(File file) throws DocumentParseException {
        String extension = FilenameUtils.getExtension(file.getName());
        if (StringUtils.isNotBlank(extension)) {
            return getParserByExtension(extension).parse(file);
        }
        // 无扩展名时用Tika检测
        try {
            Tika tika = new Tika();
            String mimeType = tika.detect(file);
            // 简化处理：根据MIME类型映射到扩展名
            MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
            String detectedExtension = allTypes.forName(mimeType).getExtension().substring(1);
            return getParserByExtension(detectedExtension).parse(file);
        } catch (Exception e) {
            throw new DocumentParseException("自动检测文档类型失败", e);
        }
    }

    public List<DocumentParseResult> autoBatchParse(File file) throws DocumentParseException {
        List<DocumentParseResult> documentParseResults = new ArrayList<>();
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File listFile : files) {
                    documentParseResults.addAll(autoBatchParse(listFile));
                }
            }
        } else {
            documentParseResults.add(autoParse(file));
        }
        return documentParseResults;
    }
}
