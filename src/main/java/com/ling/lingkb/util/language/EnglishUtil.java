package com.ling.lingkb.util.language;

import com.ling.lingkb.entity.FeatureExtractResult;
import com.ling.lingkb.entity.Language;
import com.ling.lingkb.llm.ModelTrainer;
import edu.stanford.nlp.ling.BasicDatum;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.PropertiesUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
    private static ThreadLocal<List<CoreLabel>> labelLocal = new ThreadLocal<>();
    private static ThreadLocal<List<CoreSentence>> sentenceLocal = new ThreadLocal<>();

    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("\\n\\s*\\n");
    private static final Pattern TOC_PATTERN =
            Pattern.compile("^(\\d+(\\.\\d+)*\\s+|Chapter\\s+\\d+\\.?\\s+|Section\\s+\\d+\\.?\\s+|\\w+\\.\\s+)(.*)$",
                    Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static JLanguageTool enLangTool = new JLanguageTool(new AmericanEnglish());
    private static StanfordCoreNLP enPipeline = new StanfordCoreNLP(PropertiesUtils
            .asProperties("annotators", "tokenize,ssplit,pos,lemma,ner,parse,sentiment", "tokenize.language",
                    Language.EN.getIsoCode()));

    static JLanguageTool getEnLangTool() {
        return enLangTool;
    }

    static void nlp(FeatureExtractResult input, boolean enableLemma, boolean enableStem, int summarySize,
                    int keywordSize) {
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
            coreLabel.setWord(word);
            input.getTermFrequency().put(word, input.getTermFrequency().getOrDefault(word, 0) + 1);
        }
        labelLocal.set(coreLabels);

        input.getMetricCount().put("charCount", text.length());
        input.getMetricCount().put("wordCount", document.tokens().size());

        input.getMetricCount().put("sentenceCount", document.sentences().size());
        List<CoreSentence> coreSentences = document.sentences();
        sentenceLocal.set(coreSentences);
        input.setSentences(coreSentences.stream().map(CoreSentence::text).collect(Collectors.toList()));

        List<String> paragraphs =
                Arrays.stream(PARAGRAPH_PATTERN.split(text)).map(String::trim).filter(p -> !p.isEmpty())
                        .collect(Collectors.toList());
        input.setParagraphs(paragraphs);

        List<String> summaries = getTopSentenceList(coreSentences, input.getTermFrequency(), summarySize);
        input.setSummaries(summaries);
        List<String> keywords = getKeywordList(input.getTermFrequency(), keywordSize);
        input.setKeywords(keywords);

        List<String> tocList =
                TOC_PATTERN.matcher(text).results().map(m -> m.group(0).trim()).collect(Collectors.toList());
        Map<String, String> tocMap = separateText(tocList);
        input.setTocMap(tocMap);
        input.setTopics(keywords);

        BasicDatum<String, String> basicDatum = createDatum(text);
        String category = ModelTrainer.CLASSIFIER_EN.classOf(basicDatum);
        input.setCategory(category);
    }

    private static List<String> getTopSentenceList(List<CoreSentence> coreSentences, Map<String, Integer> termFrequency,
                                                   int summarySize) {
        Map<String, Double> sentenceScores = new HashMap<>(coreSentences.size());
        for (CoreSentence sentence : coreSentences) {
            double score = 0;
            for (CoreMap token : sentence.tokens()) {
                String word = token.get(CoreAnnotations.LemmaAnnotation.class).toLowerCase();
                score += termFrequency.getOrDefault(word, 0);
            }
            sentenceScores.put(sentence.text(), score);
        }
        return sentenceScores.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(summarySize).map(Map.Entry::getKey).collect(Collectors.toList());
    }

    private static List<String> getKeywordList(Map<String, Integer> termFrequency, int keywordSize) {
        return termFrequency.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(keywordSize).map(Map.Entry::getKey).collect(Collectors.toList());
    }

    private static Map<String, String> separateText(List<String> tocList) {
        Map<String, String> tocMap = new LinkedHashMap<>();
        for (String title : tocList) {
            String anchor = title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
            tocMap.put(title, anchor);
        }
        return tocMap;
    }

    private static BasicDatum<String, String> createDatum(String text) {
        List<String> features = Arrays.asList(text.toLowerCase().split("\\s+"));
        return new BasicDatum<>(features, null);
    }

    static void posTags(FeatureExtractResult input) {
        List<CoreLabel> tokens = labelLocal.get();
        Map<String, Set<String>> posMap = new TreeMap<>();
        if (tokens != null) {
            for (CoreLabel token : tokens) {
                String word = token.word();
                String posTag = token.tag();
                posMap.computeIfAbsent(word, k -> new HashSet<>()).add(posTag);
            }
        }
        input.setPosTags(posMap);
    }

    static void ner(FeatureExtractResult input) {
        List<CoreLabel> tokens = labelLocal.get();
        Map<String, Set<String>> namedEntities = new TreeMap<>();
        if (tokens != null) {
            for (CoreLabel token : tokens) {
                String ner = token.ner();
                namedEntities.computeIfAbsent(ner, k -> new HashSet<>()).add(token.word());
            }
        }
        input.setNamedEntities(namedEntities);
    }

    static void sentimentPolarity(FeatureExtractResult input) {
        List<CoreSentence> sentences = sentenceLocal.get();
        int polarity = 4;
        if (sentences != null) {
            for (CoreSentence coreSentence : sentences) {
                String sentiment = coreSentence.sentiment();
                int sentencePolarity;
                switch (sentiment) {
                    case "Very Negative":
                        sentencePolarity = 0;
                        break;
                    case "Negative":
                        sentencePolarity = 1;
                        break;
                    case "Neutral":
                        sentencePolarity = 2;
                        break;
                    case "Positive":
                        sentencePolarity = 3;
                        break;
                    default:
                        sentencePolarity = 4;
                }
                polarity = Math.min(polarity, sentencePolarity);
            }
        }
        input.setSentimentPolarity(polarity);
    }
}
