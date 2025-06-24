package com.ling.lingkb.data.clean;

import com.alibaba.fastjson.JSONObject;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * A utility class for processing document structure, including:
 * 1. Removal of headers and footers
 * 2. Extraction and separation of table of contents/index
 * 3. Handling of footnotes/endnotes
 * 4. Deletion of watermarks/copyright information
 * 5. Paragraph reorganization (combining consecutive empty lines)
 * 6. Heading level normalization (standardizing heading formats)
 * 7. List item processing (unifying list formats)
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/24
 */
@Slf4j
@Component
@Data
@EqualsAndHashCode(callSuper = false)
@ConfigurationProperties(prefix = "data.clean.structure")
public class StructureOptimizer extends AbstractTextCleaner {
    private boolean enableOptimizer = true;
    private boolean tocOptimizer = false;

    private static final Pattern HEADER_FOOTER_PATTERN =
            Pattern.compile("^.*?(?=\r?\n\r?\n)|(?<=\r?\n\r?\n).*?$", Pattern.MULTILINE | Pattern.DOTALL);

    private static final Pattern TOC_TITLE_PATTERN =
            Pattern.compile("^(.*?[目录|TOC].*?)$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    private static final Pattern TOC_ITEM_PATTERN = Pattern.compile("^(.*?)\\.+\\s*(\\d+)$", Pattern.MULTILINE);

    private static final Pattern FOOTNOTE_REF_PATTERN = Pattern.compile("\\[([0-9]+)\\]");

    private static final Pattern FOOTNOTE_CONTENT_PATTERN = Pattern.compile("^\\[([0-9]+)\\](.*)$", Pattern.MULTILINE);

    private static final Pattern WATERMARK_PATTERN =
            Pattern.compile(".*(版权|COPYRIGHT|CONFIDENTIAL).*", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private static final List<Pattern> TITLE_PATTERNS =
            Arrays.asList(Pattern.compile("^第([零一二三四五六七八九十百千0-9]+)[章节].*$"), Pattern.compile("^([0-9]+)\\..*$"),
                    Pattern.compile("^([0-9]+)\\.([0-9]+)\\..*$"),
                    Pattern.compile("^([0-9]+)\\.([0-9]+)\\.([0-9]+)\\..*$"));

    private static final Pattern LIST_ITEM_PATTERN =
            Pattern.compile("^(\\d+\\.|•|\\-|\\*|\\+|\\)|\\])\\s+(.*)$", Pattern.MULTILINE);

    @Override
    protected String doClean(String text) {
        log.info("Processing document structure...");
        if (!enableOptimizer || StringUtils.isBlank(text)) {
            return text;
        }
        String processedText = text;
        processedText = removeHeaderFooter(processedText);
        processedText = removeWatermark(processedText);
        processedText = processFootnotes(processedText);
        processedText = reorganizeParagraphs(processedText);
        processedText = normalizeHeadings(processedText);
        processedText = processListItems(processedText);
        if (tocOptimizer) {
            processedText = extractTableOfContents(text);
        }

        return processedText;
    }

    private static String removeHeaderFooter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        Matcher matcher = HEADER_FOOTER_PATTERN.matcher(text);
        return matcher.replaceAll("");
    }

    public static String extractTableOfContents(String text) {
        StringBuilder tocBuilder = new StringBuilder();
        StringBuilder contentBuilder = new StringBuilder();
        boolean inTocSection = false;

        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            if (TOC_TITLE_PATTERN.matcher(line).matches()) {
                inTocSection = true;
                tocBuilder.append(line).append("\n");
                continue;
            }

            if (inTocSection) {
                if (line.trim().isEmpty()) {
                    inTocSection = false;
                    contentBuilder.append(line).append("\n");
                } else if (TOC_ITEM_PATTERN.matcher(line).matches()) {
                    tocBuilder.append(line).append("\n");
                } else {
                    inTocSection = false;
                    contentBuilder.append(line).append("\n");
                }
            } else {
                contentBuilder.append(line).append("\n");
            }
        }

        return new JSONObject().fluentPut("toc", tocBuilder.toString()).fluentPut("content", contentBuilder.toString())
                .toJSONString();
    }

    private static String processFootnotes(String text) {
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

    private static String removeWatermark(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        Matcher matcher = WATERMARK_PATTERN.matcher(text);
        return matcher.replaceAll("");
    }

    private static String reorganizeParagraphs(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.replaceAll("(\\r?\\n){3,}", "\n\n");
    }

    private static String normalizeHeadings(String text) {
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

    private static int detectHeadingLevel(String line) {
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

    private static String processListItems(String text) {
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