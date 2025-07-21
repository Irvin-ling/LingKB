package com.ling.lingkb.llm.data.processor;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A utility class for processing document structure, including:
 * 1. Removal of headers and footers
 * 2. Handling of footnotes/endnotes
 * 3. Deletion of watermarks/copyright information
 * 4. Paragraph reorganization (combining consecutive empty lines)
 * 5. Heading level normalization (standardizing heading formats)
 * 6. List item processing (unifying list formats)
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/24
 */
@Slf4j
@Component
public class StructureProcessor extends AbstractTextProcessor {
    @Value("${data.processor.structure.enable}")
    private boolean dataStructureEnable;

    private static final Pattern HEADER_FOOTER_PATTERN =
            Pattern.compile("^.*?(?=\r?\n\r?\n)|(?<=\r?\n\r?\n).*?$", Pattern.MULTILINE | Pattern.DOTALL);

    private static final Pattern FOOTNOTE_REF_PATTERN = Pattern.compile("\\[([0-9]+)\\]");

    private static final Pattern FOOTNOTE_CONTENT_PATTERN = Pattern.compile("^\\[([0-9]+)\\](.*)$", Pattern.MULTILINE);

    private static final Pattern WATERMARK_PATTERN = Pattern.compile("(?i)(版权|COPYRIGHT|CONFIDENTIAL)[^\\n]*\\n?");

    private static final List<Pattern> TITLE_PATTERNS =
            Arrays.asList(Pattern.compile("^第([零一二三四五六七八九十百千0-9]+)[章节].*$"), Pattern.compile("^([0-9]+)\\..*$"),
                    Pattern.compile("^([0-9]+)\\.([0-9]+)\\..*$"),
                    Pattern.compile("^([0-9]+)\\.([0-9]+)\\.([0-9]+)\\..*$"));

    private static final Pattern LIST_ITEM_PATTERN =
            Pattern.compile("^(\\d+\\.|•|\\-|\\*|\\+|\\)|\\])\\s+(.*)$", Pattern.MULTILINE);

    @Override
    String doProcess(String text) {
        log.info("StructureProcessor.doProcess()...");
        if (dataStructureEnable) {
            text = removeHeaderFooter(text);
            text = removeWatermark(text);
            text = processFootnotes(text);
            text = reorganizeParagraphs(text);
            text = normalizeHeadings(text);
            text = processListItems(text);
        }
        return text;
    }

    private String removeHeaderFooter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        Matcher matcher = HEADER_FOOTER_PATTERN.matcher(text);
        return matcher.replaceAll("");
    }

    private String processFootnotes(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        Matcher refMatcher = FOOTNOTE_REF_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (refMatcher.find()) {
            String refNum = refMatcher.group(1);
            refMatcher.appendReplacement(sb, "[^" + refNum + "]");
        }
        refMatcher.appendTail(sb);

        Matcher contentMatcher = FOOTNOTE_CONTENT_PATTERN.matcher(sb.toString());
        return contentMatcher.replaceAll("");
    }

    private String removeWatermark(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        Matcher matcher = WATERMARK_PATTERN.matcher(text);
        return matcher.replaceAll("");
    }

    private String reorganizeParagraphs(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.replaceAll("(\\r?\\n){3,}", "\n\n");
    }

    private String normalizeHeadings(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        String[] lines = text.split("\\r?\\n");

        for (String line : lines) {
            int level = detectHeadingLevel(line);
            if (level > 0) {
                result.append("<h").append(level).append(">").append(line.trim()).append("</h").append(level)
                        .append(">\n");
            } else {
                result.append(line).append("\n");
            }
        }

        return result.toString();
    }

    private int detectHeadingLevel(String line) {
        if (line == null || line.trim().isEmpty()) {
            return 0;
        }

        for (int i = 0; i < TITLE_PATTERNS.size(); i++) {
            if (TITLE_PATTERNS.get(i).matcher(line).matches()) {
                return i + 1;
            }
        }
        return 0;
    }

    private String processListItems(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        Matcher matcher = LIST_ITEM_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(sb, "• " + matcher.group(2));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }
}