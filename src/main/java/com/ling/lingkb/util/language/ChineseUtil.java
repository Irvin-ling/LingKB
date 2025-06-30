package com.ling.lingkb.util.language;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.occurrence.TermFrequency;
import com.hankcs.hanlp.dictionary.stopword.CoreStopWordDictionary;
import com.hankcs.hanlp.mining.word.TermFrequencyCounter;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.summary.TextRankKeyword;
import com.hankcs.hanlp.summary.TextRankSentence;
import com.hankcs.hanlp.tokenizer.NLPTokenizer;
import com.ling.lingkb.entity.FeatureExtractResult;
import com.ling.lingkb.llm.ModelTrainer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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

    private static ThreadLocal<List<Term>> termLocal = new ThreadLocal<>();
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("(\\n\\s*\\n)|(^\\s{2,})");
    private static final Pattern TOC_PATTERN =
            Pattern.compile("^\\s*(\\d+\\..+|第[一二三四五六七八九十]+章\\s+.+)", Pattern.MULTILINE);
    private static JLanguageTool zhLangTool = new JLanguageTool(new Chinese());

    static JLanguageTool getZhLangTool() {
        return zhLangTool;
    }

    private static final double VERY_NEGATIVE_THRESHOLD = -2.0;
    private static final double NEGATIVE_THRESHOLD = -0.5;
    private static final double POSITIVE_THRESHOLD = 0.5;
    private static final double VERY_POSITIVE_THRESHOLD = 2.0;
    private static final Map<String, Double> SENTIMENT_DICT = new HashMap<>();
    private static final Map<String, Double> DEGREE_DICT = new HashMap<>();
    private static final Set<String> NEGATION_WORDS = new HashSet<>();

    static {
        // 初始化情感词典
        SENTIMENT_DICT.put("好", 2.0);
        SENTIMENT_DICT.put("棒", 3.0);
        SENTIMENT_DICT.put("优秀", 3.5);
        SENTIMENT_DICT.put("喜欢", 2.5);
        SENTIMENT_DICT.put("满意", 2.0);
        SENTIMENT_DICT.put("差", -2.0);
        SENTIMENT_DICT.put("坏", -2.5);
        SENTIMENT_DICT.put("垃圾", -3.5);
        SENTIMENT_DICT.put("讨厌", -3.0);
        SENTIMENT_DICT.put("失望", -3.0);

        // 初始化程度副词词典
        DEGREE_DICT.put("非常", 1.8);
        DEGREE_DICT.put("很", 1.5);
        DEGREE_DICT.put("极其", 2.0);
        DEGREE_DICT.put("比较", 0.7);
        DEGREE_DICT.put("有点", 0.5);
        DEGREE_DICT.put("稍微", 0.3);

        // 初始化否定词词典
        NEGATION_WORDS.add("不");
        NEGATION_WORDS.add("没");
        NEGATION_WORDS.add("无");
        NEGATION_WORDS.add("非");
        NEGATION_WORDS.add("未");
    }

    static void nlp(FeatureExtractResult input, int summarySize, int keywordSize, int topicSize) {
        String text = input.getProcessedText();
        List<Term> terms = HanLP.segment(text);
        CoreStopWordDictionary.apply(terms);
        termLocal.set(terms);
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

    static void posTags(FeatureExtractResult input) {
        List<Term> terms = termLocal.get();
        if (terms != null) {
            Map<String, Set<String>> posMap = new TreeMap<>();
            for (Term term : terms) {
                String nature = term.nature == null ? null : term.nature.toString();
                posMap.computeIfAbsent(term.word, k -> new HashSet<>()).add(nature);
            }
            input.setPosTags(posMap);
        }
    }

    static void ner(FeatureExtractResult input) {
        Map<String, Set<String>> namedEntities = new TreeMap<>();
        List<Term> termList = NLPTokenizer.segment(input.getProcessedText());

        for (Term term : termList) {
            String word = term.word;
            String nature = term.nature == null ? "" : term.nature.toString();
            if (nature.startsWith("nr")) {
                namedEntities.computeIfAbsent("人物", k -> new HashSet<>()).add(word);
            } else if (nature.startsWith("ns")) {
                namedEntities.computeIfAbsent("地点", k -> new HashSet<>()).add(word);
            } else if (nature.startsWith("nt")) {
                namedEntities.computeIfAbsent("机构", k -> new HashSet<>()).add(word);
            } else if (nature.startsWith("nz") || nature.startsWith("nx")) {
                namedEntities.computeIfAbsent("产品/术语", k -> new HashSet<>()).add(word);
            }
        }
        input.setNamedEntities(namedEntities);
    }

    static void sentimentPolarity(FeatureExtractResult input) {
        List<Term> terms = termLocal.get();
        int polarity = getSentimentRating(terms);
        input.setSentimentPolarity(polarity);
    }

    /**
     * 计算文本的情感得分
     */
    private static double calculateSentimentScore(List<Term> words) {
        double score = 0.0;
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i).word;
            // 检查是否为情感词
            if (SENTIMENT_DICT.containsKey(word)) {
                double currentScore = SENTIMENT_DICT.get(word);
                double degree = 1.0;
                int negationCount = 0;

                // 向前查找程度副词和否定词
                int j = i - 1;
                while (j >= 0 &&
                        (DEGREE_DICT.containsKey(words.get(j).word) || NEGATION_WORDS.contains(words.get(j).word))) {
                    String prevWord = words.get(j).word;

                    if (DEGREE_DICT.containsKey(prevWord)) {
                        degree *= DEGREE_DICT.get(prevWord);
                    }

                    if (NEGATION_WORDS.contains(prevWord)) {
                        negationCount++;
                    }

                    j--;
                }

                if (negationCount % 2 == 1) {
                    currentScore = -currentScore;
                }

                currentScore *= degree;
                score += currentScore;
            }
        }

        return score;
    }

    /**
     * 将情感得分映射到0-4的评分
     */
    private static int getSentimentRating(List<Term> terms) {
        double score = calculateSentimentScore(terms);
        if (score <= VERY_NEGATIVE_THRESHOLD) {
            return 0;
        } else if (score <= NEGATIVE_THRESHOLD) {
            return 1;
        } else if (score <= POSITIVE_THRESHOLD) {
            return 2;
        } else if (score <= VERY_POSITIVE_THRESHOLD) {
            return 3;
        } else {
            return 4;
        }
    }
}
