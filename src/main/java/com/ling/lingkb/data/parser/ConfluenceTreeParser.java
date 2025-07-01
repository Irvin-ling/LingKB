package com.ling.lingkb.data.parser;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ling.lingkb.entity.DocumentParseResult;
import com.ling.lingkb.exception.DocumentParseException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Confluence Tree Parser
 * <p>
 * Parses content from Confluence space links with authentication support
 * </p>
 * <strong>The path URL needs to include authentication parameters</strong>
 *
 * @author shipotian
 * @version 1.0.0
 */
@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "data.parser.confluence")
public class ConfluenceTreeParser implements DocumentParser {
    private int maxContentLength = 10_000_000;
    private int timeoutMs = 30_000;
    private int maxDepth = 5;
    private static final Pattern PAGE_ID_PATTERN = Pattern.compile("/pages/(\\d+)/");

    private final Set<String> visitedUrls = new HashSet<>();
    private String username;
    private String password;
    private String authToken;
    private AuthType authType = AuthType.NONE;

    public enum AuthType {
        /**
         * Four authentication methods for Confluence
         */
        NONE, BASIC, COOKIE, TOKEN
    }

    /**
     * Set basic authentication credentials
     */
    private void setBasicAuthCredentials(String username, String password) {
        this.username = username;
        this.password = password;
        this.authType = AuthType.BASIC;
    }

    /**
     * Set cookie-based authentication token
     */
    private void setCookieAuth(String authToken) {
        this.authToken = authToken;
        this.authType = AuthType.COOKIE;
    }

    /**
     * Set API token authentication
     */
    private void setTokenAuth(String authToken) {
        this.authToken = authToken;
        this.authType = AuthType.TOKEN;
    }

    @Override
    public DocumentParseResult parse(String rootUrl) throws DocumentParseException {
        log.info("ConfluenceTreeParser.parse({})...", rootUrl);
        visitedUrls.clear();
        init(rootUrl);
        DocumentParseResult result = new DocumentParseResult();
        StringBuilder combinedContent = new StringBuilder();
        DocumentParseResult.DocumentMetadata metadata = DocumentParseResult.DocumentMetadata.builder().build();
        try {
            parseRecursive(rootUrl, combinedContent, metadata, 0);
            // Apply length limit
            if (combinedContent.length() > maxContentLength) {
                log.warn("current file content truncated due to size limit {}", maxContentLength);
                result.setText(
                        combinedContent.substring(0, maxContentLength) + "...[content truncated due to size limit]");
            } else {
                result.setText(combinedContent.toString());
            }
            result.setMetadata(metadata);
        } catch (Exception e) {
            throw new DocumentParseException("Failed to parse Confluence tree: " + rootUrl, e);
        }

        return result;
    }

    private void init(String urlString) throws DocumentParseException {
        URL url;
        try {
            url = new URL(urlString);
            String query = url.getQuery();
            Map<String, String> params = new HashMap<>();
            if (query != null) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    int idx = pair.indexOf("=");
                    if (idx != -1) {
                        String key = pair.substring(0, idx);
                        String value = pair.substring(idx + 1);
                        params.put(key, value);
                    }
                }
            }
            authType = AuthType.valueOf(params.getOrDefault("authType", "NONE"));
            switch (authType) {
                case BASIC: {
                    setBasicAuthCredentials(params.get("username"), params.get("password"));
                    break;
                }
                case TOKEN: {
                    setTokenAuth(params.get("authToken"));
                    break;
                }
                case COOKIE: {
                    setCookieAuth(params.get("authToken"));
                    break;
                }
                default: {
                }
            }
        } catch (MalformedURLException e) {
            throw new DocumentParseException("Wrong URL!");
        }
    }

    private void parseRecursive(String url, StringBuilder combinedContent,
                                DocumentParseResult.DocumentMetadata metadata, int depth) throws IOException {
        if (depth > maxDepth || visitedUrls.contains(url)) {
            return;
        }

        visitedUrls.add(url);
        Document doc = fetchDocumentWithAuth(url);
        log.info("successfully resolved: {}", url);

        if (StringUtils.isBlank(metadata.getAuthor())) {
            metadata.setAuthor(getAuthor(doc));
            metadata.setCreationDate(0);
            metadata.setSourceFileName(url);
        }

        // Add current page content
        metadata.setPageCount(metadata.getPageCount() + 1);
        combinedContent.append("=== Page: ").append(doc.title()).append(" ===\n").append(parseConfluenceContent(doc))
                .append("\n\n");

        // Recursive processing of subPages
        if (depth < maxDepth) {
            for (String childUrl : findChildPageLinks(url)) {
                parseRecursive(childUrl, combinedContent, metadata, depth + 1);
            }
        }
    }

    private Document fetchDocumentWithAuth(String url) throws IOException {
        switch (authType) {
            case BASIC:
                return Jsoup.connect(url).timeout(timeoutMs).userAgent("Mozilla/5.0").header("Authorization",
                        "Basic " + java.util.Base64.getEncoder().encodeToString((username + ":" + password).getBytes()))
                        .get();
            case COOKIE:
                return Jsoup.connect(url).timeout(timeoutMs).userAgent("Mozilla/5.0").cookie("JSESSIONID", authToken)
                        .get();
            case TOKEN:
                return Jsoup.connect(url).timeout(timeoutMs).userAgent("Mozilla/5.0")
                        .header("Authorization", "Bearer " + authToken).get();
            case NONE:
            default:
                return Jsoup.connect(url).timeout(timeoutMs).userAgent("Mozilla/5.0").get();
        }
    }

    private String fetchStringWithAuth(String url) throws IOException {
        switch (authType) {
            case BASIC:
                return Jsoup.connect(url).timeout(timeoutMs).userAgent("Mozilla/5.0").header("Authorization",
                        "Basic " + java.util.Base64.getEncoder().encodeToString((username + ":" + password).getBytes()))
                        .method(Connection.Method.GET).ignoreContentType(true).execute().body();
            case COOKIE:
                return Jsoup.connect(url).timeout(timeoutMs).userAgent("Mozilla/5.0").cookie("JSESSIONID", authToken)
                        .method(Connection.Method.GET).ignoreContentType(true).execute().body();
            case TOKEN:
                return Jsoup.connect(url).timeout(timeoutMs).userAgent("Mozilla/5.0")
                        .header("Authorization", "Bearer " + authToken).method(Connection.Method.GET)
                        .ignoreContentType(true).execute().body();
            case NONE:
            default:
                return Jsoup.connect(url).timeout(timeoutMs).userAgent("Mozilla/5.0").method(Connection.Method.GET)
                        .ignoreContentType(true).execute().body();
        }
    }

    private String parseConfluenceContent(Document doc) {
        // Confluence-specific content extraction
        // Try to find the main content area
        String content = doc.select(".wiki-content").text();
        if (content.isEmpty()) {
            content = doc.select("#content").text();
        }
        if (content.isEmpty()) {
            content = doc.body().text();
        }
        return content;
    }

    private List<String> findChildPageLinks(String baseUrl) throws IOException {
        List<String> childUrls = new ArrayList<>();
        String pageId = extractPageIdFromUrl(baseUrl);

        if (pageId == null) {
            return childUrls;
        }

        String apiUrl = buildAbsoluteUrl(baseUrl, String.format("/rest/api/content/%s/child/page?limit=100", pageId));
        String response = fetchStringWithAuth(apiUrl);

        List<JSONObject> list = JSON.parseObject(response).getJSONArray("results").toJavaList(JSONObject.class);
        log.info("pageId({}) find {} child links", pageId, list.size());
        for (JSONObject obj : list) {
            String childPageId = obj.getString("id");
            String childPageUrl =
                    buildAbsoluteUrl(baseUrl, String.format("/pages/viewpage.action?pageId=%s", childPageId));
            childUrls.add(childPageUrl);
        }
        return childUrls;
    }

    private String extractPageIdFromUrl(String url) {
        try {
            Matcher matcher = PAGE_ID_PATTERN.matcher(url);
            if (matcher.find()) {
                return matcher.group(1);
            }

            // Try to extract from query parameters
            URL urlObj = new URL(url);
            String query = urlObj.getQuery();
            if (query != null) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    if (pair.startsWith("pageId=")) {
                        return pair.substring("pageId=".length());
                    }
                }
            }
            return null;
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private String buildAbsoluteUrl(String baseUrl, String relativePath) {
        try {
            URL url = new URL(baseUrl);
            return new URL(url.getProtocol(), url.getHost(), url.getPort(), relativePath).toString();
        } catch (Exception e) {
            return baseUrl + relativePath;
        }
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
        return Set.of("confluence");
    }
}