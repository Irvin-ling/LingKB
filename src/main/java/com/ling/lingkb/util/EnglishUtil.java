package com.ling.lingkb.util;

import com.ling.lingkb.entity.Language;
import com.ling.lingkb.entity.LingDocument;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.PropertiesUtils;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.tartarus.snowball.ext.PorterStemmer;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/25
 */
class EnglishUtil {
    private static StanfordCoreNLP pipeline = new StanfordCoreNLP(PropertiesUtils
            .asProperties("annotators", "tokenize,ssplit,pos,lemma,ner,parse,sentiment", "tokenize.language",
                    Language.EN.getIsoCode()));

    static List<String> getSentences(String text) {
        CoreDocument coreDocument = pipeline.processToCoreDocument(text);
        return coreDocument.sentences().stream().map(CoreSentence::text).collect(Collectors.toList());
    }

    static String synonymRewrite(String text) {
        // Do some logical events you want to do
        return text;
    }

    static void lemmaOrStem(LingDocument document, boolean enableLemma, boolean enableStem) {
        String text = document.getText();
        CoreDocument coreDocument = pipeline.processToCoreDocument(text);
        List<CoreLabel> coreLabels = coreDocument.tokens();
        PorterStemmer porterStemmer = new PorterStemmer();
        for (CoreLabel coreLabel : coreLabels) {
            String rawWord = coreLabel.word();
            String word = rawWord;
            if (enableLemma) {
                word = coreLabel.lemma();
            } else if (enableStem) {
                porterStemmer.setCurrent(word);
                if (porterStemmer.stem()) {
                    word = porterStemmer.getCurrent();
                }
            }
            if (!rawWord.equals(word)) {
                text = text.replaceAll(rawWord, word);
            }
        }
        document.setText(text);
    }

    static void statistics(LingDocument document) {
        String text = document.getText();
        CoreDocument coreDocument = pipeline.processToCoreDocument(text);
        document.setCharCount(text.length());
        document.setWordCount(coreDocument.tokens().size());
        document.setSentenceCount(coreDocument.sentences().size());
    }

    static void keywords(LingDocument document, int keywordSize) {
        String text = document.getText();
        CoreDocument coreDocument = pipeline.processToCoreDocument(text);
        List<CoreLabel> tokens = coreDocument.tokens();
        Map<String, Long> wordCountMap =
                tokens.stream().map(token -> token.word().toLowerCase()).filter(StringUtils::isNotBlank)
                        .collect(Collectors.groupingBy(word -> word, Collectors.counting()));
        String keywords = wordCountMap.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(keywordSize).map(Map.Entry::getKey).collect(Collectors.joining(","));
        document.setKeywords(keywords);
    }
}
