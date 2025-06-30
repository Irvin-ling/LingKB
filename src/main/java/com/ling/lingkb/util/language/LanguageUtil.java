package com.ling.lingkb.util.language;


import com.ling.lingkb.entity.FeatureExtractResult;
import com.ling.lingkb.entity.Language;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.languagetool.JLanguageTool;
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
    public final static String SPACE = " ";
    private static LanguageDetector detector;

    static {
        try {
            List<LanguageProfile> profiles = new LanguageProfileReader().readAllBuiltIn();
            detector = LanguageDetectorBuilder.create(NgramExtractors.standard()).withProfiles(profiles).build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load language profiles", e);
        }
    }

    public static Language detectLanguage(String text) {
        String isoCode = detector.detect(text).toJavaUtil().map(LdLocale::getLanguage).orElse(null);
        return Language.safeValueOf(isoCode);
    }

    public static void correct(FeatureExtractResult input) {
        try {
            JLanguageTool langTool = input.isChinese() ? ChineseUtil.getZhLangTool() : EnglishUtil.getEnLangTool();
            String text = input.getProcessedText();
            List<RuleMatch> matches = langTool.check(text);
            if (matches.isEmpty()) {
                return;
            }

            StringBuilder corrected = new StringBuilder(text);
            for (int i = matches.size() - 1; i >= 0; i--) {
                RuleMatch match = matches.get(i);
                List<String> suggestions = match.getSuggestedReplacements();

                if (!suggestions.isEmpty()) {
                    int fromPos = Math.min(match.getFromPos(), corrected.length());
                    int toPos = Math.min(match.getToPos(), corrected.length());
                    corrected.replace(fromPos, toPos, suggestions.get(0));
                }
            }
            input.setProcessedText(corrected.toString());
        } catch (IOException e) {
            log.error("Error during grammar correction: {}", e.getMessage(), e);
            throw new RuntimeException("Grammar correction failed", e);
        }
    }

    public static void nlp(FeatureExtractResult input, boolean enableLemma, boolean enableStem, int summarySize,
                           int keywordSize, int topicSize) {
        if (input.isChinese()) {
            ChineseUtil.nlp(input, summarySize, keywordSize, topicSize);
        } else if (input.isEnglish()) {
            EnglishUtil.nlp(input, enableLemma, enableStem, summarySize, keywordSize);
        }
    }

    public static void semantic(FeatureExtractResult input) {
        if (input.isChinese()) {
            ChineseUtil.posTags(input);
            ChineseUtil.ner(input);
            ChineseUtil.sentimentPolarity(input);
        } else if (input.isEnglish()) {
            EnglishUtil.posTags(input);
            EnglishUtil.ner(input);
            EnglishUtil.sentimentPolarity(input);
        }
    }

}
