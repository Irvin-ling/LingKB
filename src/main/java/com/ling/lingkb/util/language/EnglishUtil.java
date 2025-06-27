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

import com.hankcs.hanlp.summary.TextRankSentence;
import com.ling.lingkb.entity.FeatureExtractResult;
import com.ling.lingkb.entity.Language;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.PropertiesUtils;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import opennlp.tools.stemmer.PorterStemmer;
import org.languagetool.JLanguageTool;
import org.languagetool.language.AmericanEnglish;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/25
 */
class EnglishUtil {
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("\\n\\s*\\n");
    private static JLanguageTool enLangTool = new JLanguageTool(new AmericanEnglish());
    private static StanfordCoreNLP enPipeline = new StanfordCoreNLP(PropertiesUtils
            .asProperties("annotators", "tokenize,ssplit,pos,lemma,ner", "tokenize.language",
                    Language.EN.getIsoCode()));

    static JLanguageTool getEnLangTool() {
        return enLangTool;
    }

    static void nlp(FeatureExtractResult input, boolean enableLemma, boolean enableStem, int summarySize,
                    int keywordSize, int topicSize) {
        String text = input.getProcessedText();
        CoreDocument document = enPipeline.processToCoreDocument(text);
        List<CoreLabel> coreLabels = document.tokens();
        PorterStemmer porterStemmer = new PorterStemmer();
        for (CoreLabel coreLabel : coreLabels) {
            String word = coreLabel.word();
            input.getStopWordsRemoved().add(word);
            if (enableLemma) {
                word = coreLabel.lemma();
                input.getStemmedOrLemmatized().add(word);
            } else if (enableStem) {
                word = porterStemmer.stem(coreLabel.word());
                input.getStemmedOrLemmatized().add(word);
            }
            input.getTermFrequency().put(word, input.getTermFrequency().getOrDefault(word, 0) + 1);
        }

        input.getMetricCount().put("charCount", text.length());
        input.getMetricCount().put("wordCount", document.tokens().size());

        input.getMetricCount().put("sentenceCount", document.sentences().size());
        input.setSentences(document.sentences().stream().map(CoreSentence::text).collect(Collectors.toList()));

        List<String> paragraphs =
                Arrays.stream(PARAGRAPH_PATTERN.split(text)).map(String::trim).filter(p -> !p.isEmpty())
                        .collect(Collectors.toList());
        input.setParagraphs(paragraphs);
        //TODO
    }
}
