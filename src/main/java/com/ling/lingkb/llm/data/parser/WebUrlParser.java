package com.ling.lingkb.llm.data.parser;

import com.ling.lingkb.entity.LingDocument;
import java.net.URL;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Web URL Parser
 * <p>
 * Parses content from doc space links and other web URLs
 * </p>
 *
 * @author shipotian
 * @version 1.0.0
 */
@Slf4j
@Component
public class WebUrlParser implements DocumentParser {
    @Value("${data.parser.max.length}")
    private int dataMaxLength;
    @Value("${data.parser.fetch.time}")
    private int dataFetchTime;

    @Override
    public LingDocument parse(String url) throws Exception {
        log.info("WebUrlParser.parse({})...", url);
        if (url == null || url.trim().isEmpty()) {
            throw new Exception("URL cannot be null or empty");
        }

        LingDocument result = new LingDocument();
        // Validate URL format
        new URL(url);

        // Fetch and parse the web page
        Document doc = Jsoup.connect(url).timeout(dataFetchTime).userAgent("Mozilla/5.0").get();
        result.setAuthor(getAuthor(doc));
        result.setSourceFileName(url);
        result.setCreationDate(0);
        result.setPageCount(1);
        // Special handling for doc pages
        String content;
        content = parseGenericWebContent(doc);
        // Apply length limit
        if (content.length() > dataMaxLength) {
            log.warn("current file-{} content truncated due to size limit {}", url, dataMaxLength);
            content = content.substring(0, dataMaxLength);
        }
        result.setText(content);
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