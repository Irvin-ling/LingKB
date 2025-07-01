package com.ling.lingkb.api;

import com.ling.lingkb.data.extractor.FeatureExtractorFactory;
import com.ling.lingkb.entity.CodeHint;
import com.ling.lingkb.entity.DocumentParseResult;
import com.ling.lingkb.entity.FeatureExtractResult;
import com.ling.lingkb.entity.TextProcessResult;
import com.ling.lingkb.data.parser.DocumentParserFactory;
import com.ling.lingkb.data.processor.TextProcessorFactory;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private FeatureExtractorFactory extractorFactory;

    @Autowired
    public DataController(DocumentParserFactory parserFactory, TextProcessorFactory processorFactory, FeatureExtractorFactory extractorFactory) {
        this.parserFactory = parserFactory;
        this.processorFactory = processorFactory;
        this.extractorFactory = extractorFactory;
    }

    @PostMapping("/txt")
    public List<FeatureExtractResult> txt(@RequestBody String txt) throws IOException {
        File file = File.createTempFile("temp", "txt");
        FileUtils.writeStringToFile(file, txt, "utf-8");
        List<DocumentParseResult> documentParseResults = parserFactory.batchParse(file);
        List<TextProcessResult> textProcessResults = processorFactory.batchProcess(documentParseResults);
        List<FeatureExtractResult> featureExtractResults = extractorFactory.batchExtract(textProcessResults);
        file.deleteOnExit();
        return featureExtractResults;
    }

    @CodeHint(value = "backend logic main entry")
    private void dataImport(File file) {
        List<DocumentParseResult> documentParseResults = parserFactory.batchParse(file);
        List<TextProcessResult> textProcessResults = processorFactory.batchProcess(documentParseResults);
        System.out.println(textProcessResults.size());
    }
}
