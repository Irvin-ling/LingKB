package com.ling.lingkb.data.extractor;

import com.ling.lingkb.entity.FeatureExtractResult;
import com.ling.lingkb.util.language.LanguageUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Semantic Feature Extraction Utility
 * <p>
 * This class specializes in extracting high-level semantic features from text by:
 * 1. Part-of-Speech (POS) Tagging:
 *    Assigning grammatical categories (e.g., nouns, verbs, adjectives) to each token
 *    in the text to enable syntactic analysis and context understanding.
 *
 * 2. Terminology and Named Entity Recognition (NER):
 *    Identifying and classifying key terms, entities (e.g., persons, organizations, locations),
 *    and domain-specific concepts to build structured knowledge representations from unstructured text.
 *
 * 3. Sentiment Analysis with Polarity Standardization:
 *    Measuring the emotional tone of text (positive, negative, neutral) and normalizing
 *    sentiment scores to a standardized scale for consistent interpretation across datasets.
 * <p>
 * These semantic features are critical for applications requiring deeper language understanding,
 * such as information extraction, document classification, and opinion mining.
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2023-06-30
 */
@Slf4j
@Component
@Data
@EqualsAndHashCode(callSuper = false)
@ConfigurationProperties(prefix = "data.extractor.primary")
public class SemanticExtractor extends AbstractFeatureExtractor {
    private boolean enable = true;

    @Override
    void doExtract(FeatureExtractResult input) {
        log.info("SemanticExtractor.doExtract()...");
        if (enable) {
            LanguageUtil.semantic(input);
        }
    }
}