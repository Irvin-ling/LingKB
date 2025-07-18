package com.ling.lingkb.llm.data.extractor;

import com.ling.lingkb.entity.LingDocument;
import com.ling.lingkb.util.LanguageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * Text Basic Feature Extraction Utility
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2023-06-24
 */
@Slf4j
@Component
public class LanguageExtractor {
    @Value("${data.extractor.correctGrammar}")
    private boolean dataCorrectGrammar;
    @Value("${language.english.lemma}")
    private boolean languageLemma;
    @Value("${language.english.stem}")
    private boolean languageStem;
    @Value("${language.keyword.size}")
    private int languageKeywordSize;

    public LingDocument doExtract(LingDocument document) {
        log.info("LanguageExtractor.doExtract()...");
        if (dataCorrectGrammar) {
            LanguageUtil.correctGrammar(document);
        }
        LanguageUtil.statistics(document, languageLemma, languageStem);
        LanguageUtil.keywords(document, languageKeywordSize);
        return document;
    }
}