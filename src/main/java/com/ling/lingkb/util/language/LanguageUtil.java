package com.ling.lingkb.util.language;


import com.ling.lingkb.common.entity.FeatureEngineeringResult;
import com.ling.lingkb.common.entity.Language;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.languagetool.JLanguageTool;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.language.Chinese;
import org.languagetool.rules.RuleMatch;

/**
 * Util for handling language words
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/24
 */
@Slf4j
public class LanguageUtil {
    private final static String SPACE = " ";
    private static LanguageDetector detector;
    private static JLanguageTool zhLangTool;
    private static JLanguageTool enLangTool;

    static {
        try {
            List<LanguageProfile> profiles = new LanguageProfileReader().readAllBuiltIn();
            detector = LanguageDetectorBuilder.create(NgramExtractors.standard()).withProfiles(profiles).build();
            zhLangTool = new JLanguageTool(new Chinese());
            enLangTool = new JLanguageTool(new AmericanEnglish());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load language profiles", e);
        }
    }

    public static Language detectLanguage(String text) {
        String isoCode = detector.detect(text).toJavaUtil().map(LdLocale::getLanguage).orElse(null);
        return Language.safeValueOf(isoCode);
    }

    public static void tokenize(FeatureEngineeringResult input) {
        List<String> tokens = new ArrayList<>();
        Language lang = input.getLanguage();
        String text = input.getCleanedTextContent();
        if (ChineseUtil.isChinese(lang)) {
            tokens = ChineseUtil.tokenize(text);
        } else if (EnglishUtil.isEnglish(lang)) {
            tokens = EnglishUtil.tokenize(text);
        }
        if (!tokens.isEmpty()) {
            input.setCleanedTextContent(String.join(SPACE, tokens));
            input.setStopWordsRemoved(tokens);
        }
    }

    public static List<String> tokenize(String text) {
        Language lang = detectLanguage(text);
        if (ChineseUtil.isChinese(lang)) {
            return ChineseUtil.tokenize(text);
        } else if (EnglishUtil.isEnglish(lang)) {
            return EnglishUtil.tokenize(text);
        }
        return Arrays.asList(text.split(SPACE));
    }

    public static void stemEn(FeatureEngineeringResult input) {
        if (EnglishUtil.isEnglish(input.getLanguage())) {
            List<String> stems = EnglishUtil.stem(input.getCleanedTextContent());
            input.setStemmedOrLemmatized(stems);
        }
    }

    public static void lemmatizeEn(FeatureEngineeringResult input) {
        if (EnglishUtil.isEnglish(input.getLanguage())) {
            List<String> lems = EnglishUtil.lemmatize(input.getCleanedTextContent());
            input.setStemmedOrLemmatized(lems);
        }
    }

    public static String grammarCorrection(String text) {
        String lang = detectLanguage(text).getIsoCode();
        if ("zh".equals(lang)) {
            return grammarCorrectionZh(text);
        } else {
            return grammarCorrectionEn(text);
        }
    }

    private static String grammarCorrectionZh(String text) {
        List<RuleMatch> matches;
        try {
            matches = zhLangTool.check(text);
            if (!matches.isEmpty()) {
                return text.substring(0, matches.get(0).getFromPos()) +
                        matches.get(0).getSuggestedReplacements().get(0) + text.substring(matches.get(0).getToPos());
            }
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Error occurred during Chinese grammar correction!", e);
        }
        return text;
    }

    private static String grammarCorrectionEn(String text) {
        List<RuleMatch> matches;
        try {
            matches = enLangTool.check(text);
            if (!matches.isEmpty()) {
                StringBuilder corrected = new StringBuilder(text);

                // 从后向前处理，避免位置偏移问题
                for (int i = matches.size() - 1; i >= 0; i--) {
                    RuleMatch match = matches.get(i);
                    List<String> suggestions = match.getSuggestedReplacements();

                    if (!suggestions.isEmpty()) {
                        int fromPos = match.getFromPos();
                        int toPos = match.getToPos();
                        corrected.replace(fromPos, toPos, suggestions.get(0));
                    }
                }
                return corrected.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Error occurred during English grammar correction!", e);
        }
        return text;
    }

    public static String textSummarization(String text) {
        //TODO 文本摘要：HanLP需要去除，統一改用StanfordCoreNLP
        return text;
    }

    public static String sentimentTendency(String text) {
        //TODO 情感倾向：HanLP需要去除，統一改用StanfordCoreNLP
        return text;
    }

    public static String classification(String text) {
        //TODO 情感倾向：HanLP需要去除，統一改用StanfordCoreNLP
        return text;
    }

}
