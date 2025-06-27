package com.ling.lingkb.api;
/*
 * ------------------------------------------------------------------
 * Copyright @ 2025 Hangzhou Ling Technology Co.,Ltd. All rights reserved.
 * ------------------------------------------------------------------
 * Product: LingKB
 * Module Name: LingKB
 * Date Created: 2025/6/25
 * Description:
 * ------------------------------------------------------------------
 * Modification History
 * DATE            Name           Description
 * ------------------------------------------------------------------
 * 2025/6/25       spt
 * ------------------------------------------------------------------
 */

import com.ling.lingkb.entity.CodeHint;
import com.ling.lingkb.entity.DocumentParseResult;
import com.ling.lingkb.entity.TextProcessResult;
import com.ling.lingkb.data.parser.DocumentParserFactory;
import com.ling.lingkb.data.processor.TextProcessorFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/25
 */
@RestController
@RequestMapping("/ling")
public class DataController {
    private DocumentParserFactory parserFactory;
    private TextProcessorFactory processorFactory;

    @Autowired
    public DataController(DocumentParserFactory parserFactory, TextProcessorFactory processorFactory) {
        this.parserFactory = parserFactory;
        this.processorFactory = processorFactory;
    }

    @CodeHint(value = "backend logic main entry")
    private void dataImport(File file) {
        List<DocumentParseResult> documentParseResults = parserFactory.batchParse(file);
        List<TextProcessResult> textProcessResults = processorFactory.batchProcess(documentParseResults);
        System.out.println(textProcessResults.size());
    }
}
