package com.ling.lingkb.data.extractor;

import com.ling.lingkb.entity.FeatureExtractResult;
import com.ling.lingkb.util.language.LanguageUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


/**
 * Text Basic Feature Extraction Utility
 * <p>
 * This class provides methods to analyze and transform text into structured features by:
 * 1. Grammar/spelling correction
 * 2. Tokenizing text into words and filtering stopwords
 * 3. Applying stemming and lemmatization to reduce words to base forms
 * 4. Calculating text metrics (length, word count, sentence count)
 * 5. Generating term frequency statistics
 * 6. Identifying sentence boundaries and paragraph structures
 * 7. Extracting text summaries, tables of contents, and indices
 * 8. Classifying text topics and building logical hierarchies
 * <p>
 * These operations lay the foundation for enhancing text comprehension by large language models
 * through structured feature engineering and context enrichment.
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2023-06-24
 */
@Slf4j
@Component
@Data
@EqualsAndHashCode(callSuper = false)
@ConfigurationProperties(prefix = "data.extractor.primary")
public class LanguageExtractor extends AbstractFeatureExtractor {
    private boolean reSplice = true;
    private boolean enableCorrect = false;
    private boolean enableLemmatize = true;
    private boolean enableStem = false;
    private int summarySize = 5;
    private int keywordSize = 10;
    private int topicSize = 3;

    @Override
    void doExtract(FeatureExtractResult input) {
        log.info("LanguageExtractor.doExtract()...");
        if (enableCorrect) {
           LanguageUtil.correct(input);
        }
        LanguageUtil.nlp(input, enableLemmatize, enableStem, summarySize, keywordSize, topicSize);

    }
}