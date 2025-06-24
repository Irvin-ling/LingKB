package com.ling.lingkb.util;


import com.google.common.base.Optional;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.PropertiesUtils;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * Util for handling language words
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/24
 */
public class LanguageUtil {
    private static LanguageDetector detector;
    private static StanfordCoreNLP pipeline;

    static {
        try {
            List<LanguageProfile> profiles = new LanguageProfileReader().readAllBuiltIn();
            detector = LanguageDetectorBuilder.create(NgramExtractors.standard()).withProfiles(profiles).build();
            pipeline = new StanfordCoreNLP(
                    PropertiesUtils.asProperties("annotators", "tokenize,ssplit,pos,lemma", "tokenize.language", "en"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load language profiles", e);
        }
    }

    public static String tokenize(String text) {
        String lang = detectLanguage(text);
        if ("zh".equals(lang)) {
            return tokenizeZh(text);
        } else {
            return tokenizeEn(text);
        }
    }

    private static String detectLanguage(String text) {
        Optional<LdLocale> op = detector.detect(text);
        if (op.isPresent()) {
            return op.get().getLanguage();
        } else {
            return null;
        }
    }

    private static String tokenizeZh(String text) {
        List<Term> terms = HanLP.segment(text);
        return terms.stream().map(term -> term.word).collect(Collectors.joining(" "));
    }

    private static String tokenizeEn(String text) {
        List<String> tokens = new ArrayList<>();
        try (Analyzer analyzer = new EnglishAnalyzer()) {
            TokenStream tokenStream = analyzer.tokenStream("field", text);
            CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                tokens.add(charTermAttribute.toString());
            }
            tokenStream.end();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return String.join(" ", tokens);
    }

    public static String stem(String text) {
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
            return text;
        }
        return String.join(" ", stems);
    }

    public static String lemmatize(String text) {
        Annotation document = new Annotation(text);
        pipeline.annotate(document);
        return document.get(CoreAnnotations.TokensAnnotation.class).stream().map(CoreLabel::lemma)
                .collect(Collectors.joining(" "));
    }
}
