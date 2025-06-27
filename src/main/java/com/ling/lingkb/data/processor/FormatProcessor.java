package com.ling.lingkb.data.processor;


import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * A utility class for advanced document format processing, including:
 * 1. Table/list flattening
 * 2. Code block special handling (marking or removal)
 * 3. Mathematical formula conversion (e.g., LaTeX to text)
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/24
 */
@Slf4j
@Component
@Data
@EqualsAndHashCode(callSuper = false)
@ConfigurationProperties(prefix = "data.processor.format")
public class FormatProcessor extends AbstractTextProcessor {
    private boolean enable = true;
    /**
     * 0-mark
     * 1-remove
     * 2-do nothing
     */
    private int codeBlockMethod = 0;
    private final static Pattern TABLE_PATTERN =
            Pattern.compile("^\\|.*\\|$\\s*^\\|?\\s*[-:]+\\s*\\|(?:\\s*[-:]+\\s*\\|)*\\s*$\\s*(?:^\\|.*\\|$\\s*)*",
                    Pattern.MULTILINE);
    private final static Pattern LIST_PATTERN =
            Pattern.compile("^(\\s*)(\\*|\\-|\\+|\\d+\\.)\\s+(.*)$", Pattern.MULTILINE);
    private final static Pattern CODE_BLOCK_PATTERN = Pattern.compile("```([\\s\\S]*?)```", Pattern.MULTILINE);

    @Override
    String doProcess(String text) {
        log.info("FormatProcessor.doProcess()...");
        if (enable) {
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
}
