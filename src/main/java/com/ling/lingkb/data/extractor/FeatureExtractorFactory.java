package com.ling.lingkb.data.extractor;

import com.ling.lingkb.entity.CodeHint;
import com.ling.lingkb.entity.FeatureExtractResult;
import com.ling.lingkb.entity.TextProcessResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Central factory for orchestrating feature extraction pipelines in the text processing workflow.
 * <p>
 * This factory initializes and configures the primary feature extraction chain, ensuring
 * consistent and reliable feature engineering across different text inputs.
 * <p>
 * Key behaviors:
 * <ul>
 * <li>Processes text through a chain of extractors to generate structured features</li>
 * <li>Handles errors gracefully by returning the original text content without exceptions</li>
 * <li>Supports both single and batch processing modes</li>
 * </ul>
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/25
 */
@Slf4j
@Component
public class FeatureExtractorFactory {

    private FeatureExtractor featureExtractor;

    @Autowired
    public FeatureExtractorFactory(LanguageExtractor languageExtractor, SemanticExtractor semanticExtractor,
                                   VectorExtractor vectorExtractor) {
        this.featureExtractor = languageExtractor.setNext(semanticExtractor).setNext(vectorExtractor);
    }

    @CodeHint
    public FeatureExtractResult extract(TextProcessResult processResult) throws IOException {
        FeatureExtractResult featureExtractResult = new FeatureExtractResult(processResult);
        featureExtractor.extract(featureExtractResult);
        log.debug("Successfully completed the extracting phase of text.");
        return featureExtractResult;
    }

    @CodeHint
    public List<FeatureExtractResult> batchExtract(List<TextProcessResult> processResults) throws IOException {
        List<FeatureExtractResult> list = new ArrayList<>();
        for (TextProcessResult processResult : processResults) {
            FeatureExtractResult extractResult = extract(processResult);
            list.add(extractResult);
        }
        return list;
    }
}
