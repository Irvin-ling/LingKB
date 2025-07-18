package com.ling.lingkb.util;


import com.ling.lingkb.entity.Language;
import com.ling.lingkb.entity.LingDocument;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.JaccardSimilarity;
import org.languagetool.JLanguageTool;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.language.Chinese;
import org.languagetool.rules.RuleMatch;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Util for handling language words
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/24
 */
@Slf4j
@Component
public class LanguageUtil {
    private static String systemLanguage;

    @Value("${system.language}")
    public void setSystemLanguage(String value) {
        systemLanguage = value;
    }

    private static boolean isChinese() {
        return Language.safeValueOf(systemLanguage) == Language.ZH;
    }

    public static String deduplicate(String rawText, double threshold) {
        JaccardSimilarity jaccardSimilarity = new JaccardSimilarity();
        List<String> textList;
        if (isChinese()) {
            textList = ChineseUtil.getSentences(rawText);
        } else {
            textList = EnglishUtil.getSentences(rawText);
        }
        List<String> result = new ArrayList<>();
        for (String text : textList) {
            if (result.stream().anyMatch(t -> jaccardSimilarity.apply(text, t) > threshold)) {
                rawText = rawText.replaceAll(text, "");
            } else {
                result.add(text);
            }
        }
        return rawText;
    }

    public static String synonymRewrite(String rawText) {
        if (isChinese()) {
            return ChineseUtil.synonymRewrite(rawText);
        } else {
            return EnglishUtil.synonymRewrite(rawText);
        }
    }

    public static void correctGrammar(LingDocument document) {
        try {
            String text = document.getText();
            JLanguageTool langTool = new JLanguageTool(isChinese() ? new Chinese() : new AmericanEnglish());
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
            document.setText(corrected.toString());
        } catch (IOException e) {
            log.error("Error during grammar correction: {}", e.getMessage(), e);
        }
    }

    public static void statistics(LingDocument document, boolean languageLemma, boolean languageStem) {
        if (isChinese()) {
            ChineseUtil.statistics(document);
        } else {
            EnglishUtil.lemmaOrStem(document, languageLemma, languageStem);
            EnglishUtil.statistics(document);
        }
    }

    public static void keywords(LingDocument document, int keywordSize) {
        if (isChinese()) {
            ChineseUtil.keywords(document, keywordSize);
        } else {
            EnglishUtil.keywords(document, keywordSize);
        }
    }
}
