package com.ling.lingkb.llm.data.processor;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * Text basic Cleaning and Normalization Utility
 * <p>
 * This class provides methods to process and normalize text by:
 * 1. Handling encoding conversions and fixing garbled characters
 * 2. Filtering special characters (HTML/XML tags, URLs, emails, emojis)
 * 3. Normalizing punctuation (full-width to half-width, unifying quotes/brackets)
 * 4. Cleaning up excessive whitespace and line breaks
 * 5. Standardizing case formatting
 * 6. Unify hyphens (convert different hyphen characters to a standard hyphen '-')
 * 7. Standardize currency symbols (e.g., convert '￥' to '¥')
 * 8. Process escape characters (e.g., convert HTML entities to corresponding characters)
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2023-06-24
 */
@Slf4j
@Component
public class PrimaryProcessor extends AbstractTextProcessor {
    @Value("${data.processor.primary.removeHtmlTags}")
    private boolean dataPrimaryRemoveHtmlTags;
    @Value("${data.processor.primary.removeUrls}")
    private boolean dataPrimaryRemoveUrls;
    @Value("${data.processor.primary.removeEmails}")
    private boolean dataPrimaryRemoveEmails;
    @Value("${data.processor.primary.removeEmojis}")
    private boolean dataPrimaryRemoveEmojis;
    @Value("${data.processor.primary.normalizePunctuation}")
    private boolean dataPrimaryNormalizePunctuation;
    @Value("${data.processor.primary.normalizeWhitespace}")
    private boolean dataPrimaryNormalizeWhitespace;
    @Value("${data.processor.primary.convertToLowercase}")
    private boolean dataPrimaryConvertToLowercase;

    private Charset targetCharset = StandardCharsets.UTF_8;

    /**
     * Regular expression pattern (precompiled to improve performance)
     */
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern EMOJI_PATTERN = Pattern.compile("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+");
    private static final Pattern MULTIPLE_WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern LINE_BREAKS_PATTERN = Pattern.compile("[\\r\\n]+");
    private static final Pattern ENTITY_PATTERN = Pattern.compile("&#(\\d+);");

    /**
     * Clean document content
     *
     * @ param document: The document object to be processed
     * @ return processed text
     */
    @Override
    String doProcess(String text) {
        log.info("PrimaryProcessor.doProcess()...");

        // 1. Encoding conversion and garbled code repair
        text = fixEncoding(text);

        // 2. Filtering special characters
        text = text.replaceAll("\\s+", "");
        if (dataPrimaryRemoveHtmlTags) {
            text = removeHtmlTags(text);
        }
        if (dataPrimaryRemoveUrls) {
            text = removeUrls(text);
        }
        if (dataPrimaryRemoveEmails) {
            text = removeEmails(text);
        }
        if (dataPrimaryRemoveEmojis) {
            text = removeEmojis(text);
        }

        // 3. Standardization of punctuation marks
        if (dataPrimaryNormalizePunctuation) {
            text = normalizePunctuation(text);
        }

        // 4. Handling of excess spaces/line breaks
        if (dataPrimaryNormalizeWhitespace) {
            text = normalizeWhitespace(text);
        }

        // 5. Unified capitalization
        if (dataPrimaryConvertToLowercase) {
            text = text.toLowerCase();
        }

        // 6. Unified hyphens
        text = unifyHyphens(text);

        // 7. Standardize currency
        text = standardizeCurrencySymbols(text);

        // 8. Process escape characters
        return processEscapeCharacters(text);
    }

    /**
     * Encoding conversion and garbled code repair
     */
    private String fixEncoding(String text) {
        List<Charset> commonCharsets =
                Arrays.asList(StandardCharsets.ISO_8859_1, Charset.forName("GBK"), StandardCharsets.UTF_16);

        // The degree of garbled initial text
        int originalMismatchCount = countMismatchCharacters(text);
        String bestResult = text;
        int minMismatchCount = originalMismatchCount;

        // Attempt to convert from every possible source encoding to the target encoding
        for (Charset sourceCharset : commonCharsets) {
            log.debug("Encoding fix, currently attempting {}:", sourceCharset);
            try {
                // Simulate error encoding process: First obtain the bytes of the source encoding, and then interpret the error encoding
                byte[] originalBytes = text.getBytes(sourceCharset);
                String misinterpretedText = new String(originalBytes, StandardCharsets.ISO_8859_1);

                // Attempt to fix: Return the incorrectly interpreted text back to its original bytes and parse it with the correct encoding
                byte[] restoredBytes = misinterpretedText.getBytes(StandardCharsets.ISO_8859_1);
                String restoredText = new String(restoredBytes, targetCharset);

                // Evaluate the repair effect
                int mismatchCount = countMismatchCharacters(restoredText);

                // If the repaired text has fewer garbled characters and no new non printable characters are introduced
                if (mismatchCount < minMismatchCount && !hasNewUnprintableChars(text, restoredText)) {
                    minMismatchCount = mismatchCount;
                    bestResult = restoredText;
                }
            } catch (Exception e) {
                // Ignore exceptions during the conversion process and continue trying other encodings
            }
        }

        // If a better encoding method is found, use the repaired text
        return (minMismatchCount < originalMismatchCount) ? bestResult : text;
    }

    /**
     * Calculate the number of garbled characters in the text (using heuristic methods)
     */
    private int countMismatchCharacters(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            // Check for common garbled characters or unprintable characters
            if (c == '?' || c == '\ufffd' || (c < 32 && c != '\t' && c != '\n' && c != '\r')) {
                count++;
            }
        }
        return count;
    }

    /**
     * Check if the repaired text introduces new non printable characters
     */
    private boolean hasNewUnprintableChars(String original, String repaired) {
        int originalUnprintable = countUnprintableChars(original);
        int repairedUnprintable = countUnprintableChars(repaired);
        return repairedUnprintable > originalUnprintable;
    }

    /**
     * Calculate the number of unprintable characters
     */
    private int countUnprintableChars(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 32 && c != '\t' && c != '\n' && c != '\r') {
                count++;
            }
        }
        return count;
    }

    /**
     * Remove HTML/XML tags
     */
    private String removeHtmlTags(String text) {
        return HTML_TAG_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * Remove URL
     */
    private String removeUrls(String text) {
        return URL_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * Remove email address
     */
    private String removeEmails(String text) {
        return EMAIL_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * Remove emoticons
     */
    private String removeEmojis(String text) {
        return EMOJI_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * Standardization of punctuation marks
     */
    private String normalizePunctuation(String text) {
        if (text == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // Full angle to half angle
            if (c == '＂') {
                sb.append('"');
            } else if (c == '＇') {
                sb.append('\'');
            } else if (c == '（') {
                sb.append('(');
            } else if (c == '）') {
                sb.append(')');
            } else if (c == '［') {
                sb.append('[');
            } else if (c == '］') {
                sb.append(']');
            } else if (c == '｛') {
                sb.append('{');
            } else if (c == '｝') {
                sb.append('}');
            } else if (c == '。') {
                sb.append('.');
            } else if (c == '，') {
                sb.append(',');
            } else if (c == '；') {
                sb.append(';');
            } else if (c == '：') {
                sb.append(':');
            } else if (c == '？') {
                sb.append('?');
            } else if (c == '！') {
                sb.append('!');
            } else if (c == '、') {
                sb.append(',');
            } else if (c == '—') {
                sb.append('-');
            } else if (c == '～') {
                sb.append('~');
            } else if (c >= 0xFF01 && c <= 0xFF5E) {
                sb.append((char) (c - 0xFEE0));
            } else {
                sb.append(c);
            }
        }

        // Unified quotation marks
        String result = sb.toString();
        result = result.replace('“', '"').replace('”', '"');
        result = result.replace('‘', '\'').replace('’', '\'');
        return result;
    }

    /**
     * Handling of excess spaces/line breaks
     */
    private String normalizeWhitespace(String text) {
        if (text == null) {
            return null;
        }

        // Replace line breaks with spaces
        text = LINE_BREAKS_PATTERN.matcher(text).replaceAll(" ");

        // Merge multiple consecutive spaces into a single space
        text = MULTIPLE_WHITESPACE_PATTERN.matcher(text).replaceAll(" ");

        // Remove the leading and trailing spaces
        return text.trim();
    }

    /**
     * Converts various hyphen characters to the standard ASCII hyphen '-'.
     * Handles en dash (–), em dash (—), minus sign (−), and other similar characters.
     */
    private static String unifyHyphens(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        // Replace en dash, em dash, minus sign, and other hyphen-like characters
        return text.replaceAll("[–—−‐‑‒–—―]", "-");
    }

    /**
     * Standardizes currency symbols to their canonical forms.
     * Specifically converts the full-width yen symbol (￥) to the half-width yen symbol (¥).
     */
    private static String standardizeCurrencySymbols(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        // Convert full-width yen symbol to half-width yen symbol
        text = text.replace("C$", "CA$");
        text = text.replace("A$", "AU$");
        return text.replace('￥', '¥');
    }

    /**
     * Processes escape characters and converts HTML entities to their corresponding characters.
     * Handles common entities such as &lt;, &gt;, &amp;, &quot;, and numeric entities (e.g., &amp;#169;).
     */
    private static String processEscapeCharacters(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Basic HTML entity conversions
        text = text.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&").replace("&quot;", "\"")
                .replace("&apos;", "'");

        // Convert numeric entities (e.g., &#169; to ©)
        Matcher matcher = ENTITY_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            int codePoint = Integer.parseInt(matcher.group(1));
            matcher.appendReplacement(sb, String.valueOf(Character.toChars(codePoint)));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }
}