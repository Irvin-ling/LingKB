package com.ling.lingkb.util.language;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.occurrence.TermFrequency;
import com.hankcs.hanlp.mining.word.TermFrequencyCounter;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.summary.TextRankKeyword;
import com.hankcs.hanlp.summary.TextRankSentence;
import com.ling.lingkb.entity.FeatureExtractResult;
import com.ling.lingkb.llm.ModelTrainer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.languagetool.JLanguageTool;
import org.languagetool.language.Chinese;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/25
 */
class ChineseUtil {
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("(\\n\\s*\\n)|(^\\s{2,})");
    private static final Pattern TOC_PATTERN =
            Pattern.compile("^\\s*(\\d+\\..+|第[一二三四五六七八九十]+章\\s+.+)", Pattern.MULTILINE);
    private static JLanguageTool zhLangTool = new JLanguageTool(new Chinese());

    static JLanguageTool getZhLangTool() {
        return zhLangTool;
    }

    static void nlp(FeatureExtractResult input, int summarySize, int keywordSize, int topicSize) {
        String text = input.getProcessedText();
        List<Term> terms = HanLP.segment(text).stream()
                .filter(term -> !ModelTrainer.stopWordsZh.contains(term.word) && !term.nature.startsWith("w"))
                .collect(Collectors.toList());
        for (Term term : terms) {
            String word = term.word;
            input.getStopWordsRemoved().add(word);
            input.getTermFrequency().put(word, input.getTermFrequency().getOrDefault(word, 0) + 1);
        }

        input.getMetricCount().put("charCount", text.length());
        input.getMetricCount().put("wordCount", terms.size());

        List<String> sentences = HanLP.extractSummary(text, Integer.MAX_VALUE);
        input.getMetricCount().put("sentenceCount", sentences.size());
        input.setSentences(sentences);

        List<String> paragraphs =
                Arrays.stream(PARAGRAPH_PATTERN.split(text)).map(String::trim).filter(p -> !p.isEmpty())
                        .collect(Collectors.toList());
        input.setParagraphs(paragraphs);

        List<String> summaries = TextRankSentence.getTopSentenceList(text, summarySize);
        input.setSummaries(summaries);
        List<String> keywords = TextRankKeyword.getKeywordList(text, keywordSize);
        input.setKeywords(keywords);

        List<String> tocList = TOC_PATTERN.matcher(text).results().map(m -> m.group(1)).collect(Collectors.toList());
        Map<String, String> tocMap = separateText(text, tocList);
        input.setTocMap(tocMap);

        TermFrequencyCounter counter = new TermFrequencyCounter();
        counter.add(terms);
        List<String> topics = counter.top(topicSize).stream().map(TermFrequency::getKey).collect(Collectors.toList());
        input.setTopics(topics);

        String category = ModelTrainer.CLASSIFIER_ZH.classify(text);
        input.setCategory(category);
    }

    private static Map<String, String> separateText(String text, List<String> tocList) {
        Map<String, String> result = new HashMap<>();
        String[] parts = text.split("\\n");
        String currentSection = null;
        StringBuilder content = new StringBuilder();

        for (String line : parts) {
            if (tocList.contains(line.trim())) {
                if (currentSection != null) {
                    result.put(currentSection, content.toString());
                }
                currentSection = line.trim();
                content = new StringBuilder();
            } else if (currentSection != null) {
                content.append(line).append("\n");
            }
        }
        if (currentSection != null) {
            result.put(currentSection, content.toString());
        }
        return result;
    }

}
