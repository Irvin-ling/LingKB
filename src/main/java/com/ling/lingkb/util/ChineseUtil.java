package com.ling.lingkb.util;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.dictionary.CoreSynonymDictionary;
import com.hankcs.hanlp.dictionary.stopword.CoreStopWordDictionary;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.summary.TextRankKeyword;
import com.hankcs.hanlp.utility.SentencesUtil;
import com.ling.lingkb.entity.LingDocument;
import java.util.List;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/25
 */
public class ChineseUtil {

    static List<String> getSentences(String text) {
        return SentencesUtil.toSentenceList(text);
    }

    static String synonymRewrite(String text) {
        return CoreSynonymDictionary.rewrite(text);
    }

    static void statistics(LingDocument document) {
        String text = document.getText();
        List<Term> terms = HanLP.segment(text);
        CoreStopWordDictionary.apply(terms);
        List<String> sentences = getSentences(text);
        document.setCharCount(text.length());
        document.setWordCount(terms.size());
        document.setSentenceCount(sentences.size());
    }

    static void keywords(LingDocument document, int keywordSize) {
        String text = document.getText();
        String keywords = String.join(",", TextRankKeyword.getKeywordList(text, keywordSize));
        document.setKeywords(keywords);
    }

}
