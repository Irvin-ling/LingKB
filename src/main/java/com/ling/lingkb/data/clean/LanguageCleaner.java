package com.ling.lingkb.data.clean;
/*
 * ------------------------------------------------------------------
 * Copyright @ 2025 Hangzhou Ling Technology Co.,Ltd. All rights reserved.
 * ------------------------------------------------------------------
 * Product: LingKB
 * Module Name: LingKB
 * Date Created: 2025/6/24
 * Description:
 * ------------------------------------------------------------------
 * Modification History
 * DATE            Name           Description
 * ------------------------------------------------------------------
 * 2025/6/24       spt
 * ------------------------------------------------------------------
 */

import com.github.houbb.opencc4j.util.ZhConverterUtil;
import com.ling.lingkb.util.LanguageUtil;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Language-specific Processing Class.
 * Features include:
 * 1. Conversion between Simplified and Traditional Chinese.
 * 2. Word segmentation and stop word filtering.
 * 3. English word stemming and lemmatization.
 * 4. Normalization of numbers and units (e.g., "100 米" → "100m").
 * 5. Standardization of time and date (e.g., "yesterday" → "2023-06-23").
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/24
 */
@Slf4j
@Component
@Data
@EqualsAndHashCode(callSuper = false)
@ConfigurationProperties(prefix = "data.clean.language")
public class LanguageCleaner extends AbstractTextCleaner {
    private boolean convertTraditionalToSimple = true;
    private boolean removeStopWords = true;
    private boolean stemEnglishWords = true;
    private boolean lemmatizeEnglishWords = true;
    private boolean normalizeNumbersAndUnits = true;
    private boolean normalizeDateTime = true;

    private static final Pattern NUMBER_WITH_UNIT_PATTERN = Pattern.compile("(\\d+)\\s*([a-zA-Z\u4e00-\u9fa5]+)");
    private static final Pattern DATE_TIME_PATTERN =
            Pattern.compile("(昨天|今天|明天|前天|后天|\\d{1,4}年\\d{1,2}月\\d{1,2}日|\\d{1,2}/\\d{1,2}/\\d{2,4})");

    @Override
    public String doClean(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // 1. 中文繁体转简体
        if (convertTraditionalToSimple) {
            text = ZhConverterUtil.toSimple(text);
        }

        // 2. Tokenization and Stop Word Filtering
        if (removeStopWords) {
            text = LanguageUtil.tokenize(text);
        }

        // 3. English Stemming and Lemmatization
        if (stemEnglishWords) {
            text = LanguageUtil.stem(text);
        }
        if (lemmatizeEnglishWords) {
            text = LanguageUtil.lemmatize(text);
        }

        // 4. Digital and Unit Normalization
        if (normalizeNumbersAndUnits) {
            text = normalizeNumbersAndUnits(text);
        }

        // 5. Time/Date Standardization
        if (normalizeDateTime) {
            text = normalizeDateTime(text);
        }

        return text;
    }

    /**
     * Digital and Unit Normalization
     */
    private String normalizeNumbersAndUnits(String text) {
        Matcher matcher = NUMBER_WITH_UNIT_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String number = matcher.group(1);
            String unit = matcher.group(2);
            if (unit.equals("米") || unit.equalsIgnoreCase("meter") || unit.equalsIgnoreCase("meters")) {
                matcher.appendReplacement(sb, number + "m");
            } else if (unit.equals("千米") || unit.equalsIgnoreCase("kilometer") || unit.equalsIgnoreCase("kilometers")) {
                matcher.appendReplacement(sb, number + "km");
            } else if (unit.equals("克") || unit.equalsIgnoreCase("gram") || unit.equalsIgnoreCase("grams")) {
                matcher.appendReplacement(sb, number + "g");
            } else if (unit.equals("千克") || unit.equalsIgnoreCase("kilogram") || unit.equalsIgnoreCase("kilograms")) {
                matcher.appendReplacement(sb, number + "kg");
            } else {
                matcher.appendReplacement(sb, number + unit);
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Time/Date Standardization
     */
    private String normalizeDateTime(String text) {
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String today = sdf.format(now);
        text = text.replace("昨天", getRelativeDate(now, -1)).replace("今天", today).replace("明天", getRelativeDate(now, 1))
                .replace("前天", getRelativeDate(now, -2)).replace("后天", getRelativeDate(now, 2));
        Matcher matcher = DATE_TIME_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String dateStr = matcher.group();
            try {
                if (dateStr.contains("年") && dateStr.contains("月") && dateStr.contains("日")) {
                    String normalized = dateStr.replace("年", "-").replace("月", "-").replace("日", "");
                    matcher.appendReplacement(sb, normalized);
                } else if (dateStr.contains("/")) {
                    String[] parts = dateStr.split("/");
                    if (parts[2].length() == 2) {
                        parts[2] = "20" + parts[2];
                    }
                    matcher.appendReplacement(sb, parts[2] + "-" + parts[0] + "-" + parts[1]);
                }
            } catch (Exception e) {
                matcher.appendReplacement(sb, dateStr);
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String getRelativeDate(Date date, int days) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        long time = date.getTime() + days * 24L * 60 * 60 * 1000;
        return sdf.format(new Date(time));
    }
}
