package com.ling.lingkb.data.parser;

import com.ling.lingkb.common.entity.DocumentParseResult;
import com.ling.lingkb.common.exception.DocumentParseException;
import java.io.IOException;
import java.net.URL;
import java.util.Set;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Web URL Parser
 * <p>
 * Parses content from doc space links and other web URLs
 * </p>
 *
 * @author shipotian
 * @since 1.0.0
 */
@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "data.parser.web")
public class WebUrlParser implements DocumentParser {
    private int maxContentLength = 10_000_000;
    private int timeoutMs = 30_000;

    @Override
    public DocumentParseResult parse(String url) throws DocumentParseException {
        if (url == null || url.trim().isEmpty()) {
            throw new DocumentParseException("URL cannot be null or empty");
        }

        DocumentParseResult result = new DocumentParseResult();

        try {
            // Validate URL format
            new URL(url);

            // Fetch and parse the web page
            Document doc = Jsoup.connect(url).timeout(timeoutMs).userAgent("Mozilla/5.0").get();

            // Special handling for doc pages
            String content;
            content = parseGenericWebContent(doc);

            // Apply length limit
            if (content.length() > maxContentLength) {
                log.warn("current file content truncated due to size limit {}", maxContentLength);
                content = content.substring(0, maxContentLength) + "...[content truncated due to size limit]";
            }

            result.setTextContent(content);
            result.setMetadata(DocumentParseResult.DocumentMetadata.builder().author(getAuthor(doc)).sourceFileName(url)
                    .creationDate(0).pageCount(1).build());

        } catch (IOException e) {
            throw new DocumentParseException("Failed to fetch URL content: " + url, e);
        } catch (Exception e) {
            throw new DocumentParseException("Unexpected error while parsing URL: " + url, e);
        }

        return result;
    }

    private String parseGenericWebContent(Document doc) {
        // Generic web content extraction; Remove unwanted elements
        doc.select("script, style, nav, footer, iframe").remove();

        // Try to get article content if available
        String content = doc.select("article").text();
        if (content.isEmpty()) {
            content = doc.select("main").text();
        }
        if (content.isEmpty()) {
            content = doc.body().text();
        }
        return content;
    }

    private String getAuthor(Document doc) {
        // Try to extract author information
        String author = doc.select("meta[name=author]").attr("content");
        if (author.isEmpty()) {
            author = doc.select(".author, .byline").text();
        }
        return author.isEmpty() ? "Unknown" : author;
    }

    @Override
    public Set<String> supportedTypes() {
        return Set.of("http", "https");
    }
}