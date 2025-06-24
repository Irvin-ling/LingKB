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

import com.alibaba.fastjson.JSONObject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * A utility class for advanced document format processing, including:
 * 1. Table/list flattening
 * 2. Code block special handling (marking or removal)
 * 3. Mathematical formula conversion (e.g., LaTeX to text)
 * 4. Separation of mixed-language text
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/24
 */
@Slf4j
@Component
@Data
@EqualsAndHashCode(callSuper = false)
@ConfigurationProperties(prefix = "data.clean.format")
public class FormatOptimizer extends AbstractTextCleaner {
    private boolean enableFormatter = true;
    /**
     * 0-mark
     * 1-remove
     * 2-do nothing
     */
    private int codeBlockMethod = 0;
    private boolean enableSeparate = false;

    private final static Pattern TABLE_PATTERN =
            Pattern.compile("^\\|.*\\|$\\s*^\\|?\\s*[-:]+\\s*\\|(?:\\s*[-:]+\\s*\\|)*\\s*$\\s*(?:^\\|.*\\|$\\s*)*",
                    Pattern.MULTILINE);
    private final static Pattern LIST_PATTERN =
            Pattern.compile("^(\\s*)(\\*|\\-|\\+|\\d+\\.)\\s+(.*)$", Pattern.MULTILINE);
    private final static Pattern CODE_BLOCK_PATTERN = Pattern.compile("```([\\s\\S]*?)```", Pattern.MULTILINE);

    @Override
    protected String doClean(String text) {
        log.info("Text content format processing...");
        if (!enableFormatter || StringUtils.isBlank(text)) {
            return text;
        }
        // 1. Table/list flattening
        text = flattenTables(text);
        text = flattenLists(text);

        // 2. Code block special handling
        if (codeBlockMethod == 0) {
            text = markCodeBlocks(text, "// CODE BLOCK");
        } else if (codeBlockMethod == 1) {
            text = removeCodeBlocks(text);
        }
        // 3. Mathematical formula conversion
        text = convertLatexToText(text);
        // 4. Separation of mixed-language text
        if (enableSeparate) {
            text = separateMixedLanguageText(text);
        }
        return text;
    }

    /**
     * Flattens tables into plain text format.
     * Converts simple Markdown-style tables to a flattened format.
     *
     * @param text the input text containing tables
     * @return the text with tables flattened into plain text
     */
    private String flattenTables(String text) {
        // Match Markdown-style tables
        Matcher matcher = TABLE_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String table = matcher.group();
            String[] lines = table.split("\\r?\\n");

            StringBuilder flattened = new StringBuilder();
            for (String line : lines) {
                if (line.contains("|---")) {
                    continue; // Skip header separator
                }
                String rowText = line.replaceAll("\\|", " ").trim();
                flattened.append(rowText).append("\n");
            }

            matcher.appendReplacement(sb, flattened.toString());
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Flattens nested lists into a linear format.
     * Converts nested Markdown-style lists to a flattened format with indentation.
     *
     * @param text the input text containing nested lists
     * @return the text with lists flattened to a linear format
     */
    private String flattenLists(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Match nested list items
        Matcher matcher = LIST_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String indent = matcher.group(1);
            String marker = matcher.group(2);
            String content = matcher.group(3);

            // Convert nested lists to indented text
            String flattenedItem = indent + "• " + content;
            matcher.appendReplacement(sb, flattenedItem);
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Marks code blocks with a specified marker.
     *
     * @param text   the input text containing code blocks
     * @param marker the marker to use for code blocks
     * @return the text with code blocks marked
     */
    private String markCodeBlocks(String text, String marker) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Match fenced code blocks (triple-backtick)
        return CODE_BLOCK_PATTERN.matcher(text).replaceAll(marker + "\n$1\n" + marker);
    }

    /**
     * Removes code blocks from the text.
     *
     * @param text the input text containing code blocks
     * @return the text with code blocks removed
     */
    private String removeCodeBlocks(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Match fenced code blocks (triple-backtick)
        return text.replaceAll("```([\\s\\S]*?)```", "");
    }

    /**
     * Converts LaTeX formulas to plain text format.
     *
     * @param text the input text containing LaTeX formulas
     * @return the text with LaTeX formulas converted to plain text
     */
    private String convertLatexToText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        // Remove LaTeX math delimiters
        text = text.replaceAll("\\$\\$(.*?)\\$\\$", "$1");
        text = text.replaceAll("\\$(.*?)\\$", "$1");
        // Basic LaTeX command conversion
        text = text.replaceAll("\\\\frac\\{(.*?)\\}\\{(.*?)\\}", "$1/$2");
        text = text.replaceAll("\\\\sqrt\\{(.*?)\\}", "sqrt($1)");
        text = text.replaceAll("\\\\sum\\s*_{(.*?)}^{(.*?)}", "sum from $1 to $2");
        text = text.replaceAll("\\\\int\\s*_{(.*?)}^{(.*?)}", "integral from $1 to $2");

        return text;
    }

    /**
     * Separates mixed-language text into different language segments.
     * This is a simplified implementation that separates Chinese and English text.
     *
     * @param text the input text containing mixed languages
     * @return a map of language codes to their respective text segments
     */
    private static String separateMixedLanguageText(String text) {
        // Simplified language detection: Chinese and English
        StringBuilder chineseText = new StringBuilder();
        StringBuilder englishText = new StringBuilder();

        for (char c : text.toCharArray()) {
            if (isChineseCharacter(c)) {
                chineseText.append(c);
            } else if (isEnglishCharacter(c)) {
                englishText.append(c);
            } else {
                // Add non-language characters to both segments
                chineseText.append(c);
                englishText.append(c);
            }
        }
        return new JSONObject().fluentPut("zh", chineseText.toString()).fluentPut("en", englishText.toString())
                .toJSONString();
    }

    private static boolean isChineseCharacter(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
                ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
                ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A;
    }

    private static boolean isEnglishCharacter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }
}
