package com.ling.lingkb.data.clean;

import com.ling.lingkb.util.SimHashUtil;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Text content noise data filter
 * <p>
 * Core features include:
 * 1. Noise data filtering (e.g., HTML tags, special symbols, non-text content)
 * 2. Duplicate text detection and removal (based on content similarity algorithms)
 * 3. Advertising/spam recognition and filtering (using keyword blacklists and text patterns)
 * 4. Sensitive information masking (protection of privacy data such as IDs, phone numbers, emails)
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/24
 */
@Slf4j
@Component
@Data
@EqualsAndHashCode(callSuper = false)
@ConfigurationProperties(prefix = "data.clean.noise")
public class NoiseCleaner extends AbstractTextCleaner {
    private boolean enableFilter = true;
    private double duplicateThreshold = 0.9;
    private int minTextLength = 5;
    /**
     * Chinese advertising keywords (covering promotions, contact information, inducement words)
     */
    private static final List<String> CHINESE_AD_KEYWORDS =
            Arrays.asList("免费领取", "点击下载", "扫码", "微信", "QQ", "暴利", "赚钱", "提现", "限时优惠", "折扣", "特价", "促销", "赠品", "秒杀",
                    "原价", "现价", "立即购买", "独家", "内部", "福利", "红包", "加微信", "私聊", "百分百");

    /**
     * 英文广告关键词
     */
    private static final List<String> ENGLISH_AD_KEYWORDS =
            Arrays.asList("free download", "click here", "limited time offer", "discount", "promotion", "cashback",
                    "buy now", "exclusive", "urgent", "contact us", "wechat", "qq", "make money", "withdraw cash",
                    "special price", "limited stock");

    /**
     * Universal advertising model (supporting Chinese and English)
     */
    private static final Pattern AD_PATTERNS = Pattern.compile(
            // Translate website links
            "https?://\\S+|www\\.\\S+|\\S+\\.(com|cn|net|org|top|xyz|vip)\\S*| " +
                    // Translate email
                    "\\w+@\\w+\\.\\w+(\\.\\w+)?| " +
                    // Translate phone numbers (supporting Chinese/English formats)
                    "1[3-9]\\d{9}|\\d{3,4}-?\\d{7,8}|\\+?\\d{1,3}-?\\d{1,4}-?\\d{1,4}");

    /**
     * Special symbol detection (commonly used symbols in advertising)
     */
    private static final Pattern SPECIAL_SYMBOLS = Pattern.compile("[！！!？?￥$★*()【】{}<>\\[\\]]");

    @Override
    protected String doClean(String text) {
        log.info("Text content noise data filter...");
        if (!enableFilter || StringUtils.isBlank(text)) {
            return text;
        }
        // 1. Noise data filtering
        text = filterShortText(text);
        // 2. Duplicate text detection and removal
        text = filterDuplicateText(text);
        // 3. Advertising/spam recognition and filtering
        text = filterSpamContent(text);
        text = desensitizeSensitiveInfo(text);

        return text;
    }

    /**
     * 1. Noise data filtering
     */
    private String filterShortText(String text) {
        if (text.length() < minTextLength) {
            log.debug("Filtered short text: {}", text);
            return "";
        }
        return text;
    }

    /**
     * 2. Duplicate Text Detection Based on SimHash
     */
    private String filterDuplicateText(String text) {
        List<String> list = SimHashUtil.deduplicate(Arrays.asList(text.split(" ")), duplicateThreshold);
        return String.join(" ", list);
    }

    /**
     * 3. Advertising/spam recognition and filtering
     */
    private String filterSpamContent(String text) {
        if (isAd(text)) {
            text = AD_PATTERNS.matcher(text).replaceAll("");
        }
        text = replaceKeywords(text, CHINESE_AD_KEYWORDS);
        text = replaceKeywords(text, ENGLISH_AD_KEYWORDS);
        text = SPECIAL_SYMBOLS.matcher(text).replaceAll("");
        text = text.replaceAll("\\s+", " ").trim();
        return text;
    }

    /**
     * Judge whether the text is an advertisement (supporting Chinese and English)
     */
    private static boolean isAd(String text) {
        // 1. Detect the density of URLs/contact information
        long urlCount = AD_PATTERNS.matcher(text).results().count();
        if (urlCount > 1) {
            return true;
        }

        // 2. Detect the density of advertising keywords
        int keywordCount = 0;
        keywordCount += countKeywords(text, CHINESE_AD_KEYWORDS);
        keywordCount += countKeywords(text, ENGLISH_AD_KEYWORDS);
        if (keywordCount > 2) {
            return true;
        }

        // 3. Detect the density of special symbols (advertisements often use a large number of exclamation marks, question marks, etc.)
        long specialCharCount = SPECIAL_SYMBOLS.matcher(text).results().count();
        if (specialCharCount > 3) {
            return true;
        }

        // 4. Detect the proportion of all-capital English (commonly used in English advertisements)
        return containsHighCapitalLetters(text);
    }

    /**
     * Replace keywords in the text
     */
    private static String replaceKeywords(String text, List<String> keywords) {
        for (String keyword : keywords) {
            // To avoid partial matching, add boundary conditions (full-width spaces for Chinese, half-width spaces for English)
            String pattern = "(?<![\\w\\p{Han}])" + keyword + "(?![\\w\\p{Han}])";
            text = text.replaceAll(pattern, "");
        }
        return text;
    }

    /**
     * Count the number of occurrences of keywords in the text
     */
    private static int countKeywords(String text, List<String> keywords) {
        int count = 0;
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Detect the proportion of uppercase letters in English text (more than 30% may be an advertisement)
     */
    private static boolean containsHighCapitalLetters(String text) {
        int upperCount = 0;
        int alphaCount = 0;

        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                alphaCount++;
                if (Character.isUpperCase(c)) {
                    upperCount++;
                }
            }
        }

        if (alphaCount == 0) {
            return false;
        }
        return (double) upperCount / alphaCount > 0.3;
    }

    /**
     * 4. Sensitive information desensitization
     */
    private String desensitizeSensitiveInfo(String text) {
        text = maskIdCard(text);
        text = maskPhone(text);
        text = maskBankCard(text);
        return text;
    }

    private static String maskIdCard(String text) {
        if (text.length() < 10) {
            return text;
        }
        return text.replaceAll("(?<=\\W|^)([\\u4e00-\\u9fa5])([\\u4e00-\\u9fa5]{1,3})(?=\\W|$)", "$1*");
    }

    private static String maskPhone(String text) {
        if (text.length() != 11) {
            return text;
        }
        return text.replaceAll("(?<!\\d)(1[3-9]\\d)\\d{4}(\\d{4})(?!\\d)", "$1****$2");
    }

    private static String maskBankCard(String text) {
        if (text.length() < 10) {
            return text;
        }
        return text.replaceAll("(?<!\\d)(\\d{4})\\d{8,11}(\\d{4})(?!\\d)", "$1 **** **** $2");
    }
}