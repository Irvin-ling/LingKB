package com.ling.lingkb.util.language;
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

import com.ling.lingkb.common.entity.Language;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.PropertiesUtils;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/25
 */
class EnglishUtil {
    private final static Language LANG = Language.EN;
    private static final Analyzer ANALYZER = new EnglishAnalyzer();
    private static StanfordCoreNLP pipeline = new StanfordCoreNLP(PropertiesUtils
            .asProperties("annotators", "tokenize,ssplit,pos,lemma", "tokenize.language", LANG.getIsoCode()));

    static boolean isEnglish(Language lang) {
        return LANG == lang;
    }

    static List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        try (TokenStream tokenStream = ANALYZER.tokenStream("field", text)) {
            CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();

            List<String> tokens = new ArrayList<>();
            while (tokenStream.incrementToken()) {
                tokens.add(charTermAttribute.toString());
            }
            return tokens;
        } catch (IOException e) {
            throw new RuntimeException("An IO exception occurred during the word segmentation process.", e);
        }
    }

    static List<String> stem(String text) {
        List<String> stems = new ArrayList<>();
        try {
            try (StandardTokenizer tokenizer = new StandardTokenizer()) {
                tokenizer.setReader(new StringReader(text));
                TokenStream tokenStream = new PorterStemFilter(tokenizer);
                CharTermAttribute charTerm = tokenStream.addAttribute(CharTermAttribute.class);
                tokenStream.reset();
                while (tokenStream.incrementToken()) {
                    stems.add(charTerm.toString());
                }
            }
        } catch (IOException e) {
            return stems;
        }
        return stems;
    }

    static List<String> lemmatize(String text) {
        Annotation document = new Annotation(text);
        pipeline.annotate(document);
        return document.get(CoreAnnotations.TokensAnnotation.class).stream().map(CoreLabel::lemma)
                .collect(Collectors.toList());
    }
}
