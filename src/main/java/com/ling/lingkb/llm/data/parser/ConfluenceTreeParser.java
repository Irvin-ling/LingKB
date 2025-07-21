package com.ling.lingkb.llm.data.parser;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ling.lingkb.entity.LingDocument;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
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

@Slf4j
@Component
public class ConfluenceTreeParser implements DocumentParser {
    @Value("${data.parser.confluence.depth}")
    private int dataConfluenceDepth;
    @Value("${data.parser.fetch.time}")
    private int dataFetchTime;
    @Value("${data.parser.max.length}")
    private int dataMaxLength;
    @Value("${data.page.break.symbols}")
    private String pageBreakSymbols;

    private static final Pattern PAGE_ID_PATTERN = Pattern.compile("/pages/(\\d+)/");
    private final Set<String> visitedUrls = new HashSet<>();
    private String username;
    private String password;
    private String authToken;
    private AuthType authType = AuthType.BASIC;

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
    public LingDocument parse(String rootUrl) throws Exception {
        log.info("ConfluenceTreeParser.parse({})...", rootUrl);
        visitedUrls.clear();
        init(rootUrl);
        LingDocument result = new LingDocument();
        StringBuilder combinedContent = new StringBuilder();
        parseRecursive(rootUrl, combinedContent, result, 0);
        result.setText(combinedContent.toString());
        return result;
    }

    private void init(String urlString) throws Exception {
        URL url = new URL(urlString);
        String query = url.getQuery();
        Map<String, String> params = new HashMap<>(4);
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
        authType = AuthType.valueOf(params.getOrDefault("authType", "BASIC"));
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
    }

    private void parseRecursive(String url, StringBuilder combinedContent, LingDocument result, int depth)
            throws IOException {
        if (depth > dataConfluenceDepth || visitedUrls.contains(url)) {
            return;
        }

        visitedUrls.add(url);
        Document doc = fetchDocumentWithAuth(url);
        log.info("successfully resolved: {}", url);

        if (StringUtils.isBlank(result.getAuthor())) {
            result.setAuthor(getAuthor(doc));
            result.setCreationDate(0);
            result.setSourceFileName(url);
        }
        // Add current page content
        result.setPageCount(result.getPageCount() + 1);
        String currentContent = parseConfluenceContent(doc);
        if (currentContent.length() > dataMaxLength) {
            log.warn("current file-{} content truncated due to size limit {}", doc.title(), dataMaxLength);
            currentContent = currentContent.substring(0, dataMaxLength);
        }
        combinedContent.append(pageBreakSymbols).append(doc.title()).append("\n").append(currentContent);

        // Recursive processing of subPages
        if (depth < dataConfluenceDepth) {
            for (String childUrl : findChildPageLinks(url)) {
                parseRecursive(childUrl, combinedContent, result, depth + 1);
            }
        }
    }

    private Document fetchDocumentWithAuth(String url) throws IOException {
        switch (authType) {
            case BASIC:
                return Jsoup.connect(url).timeout(dataFetchTime).userAgent("Mozilla/5.0").header("Authorization",
                        "Basic " + java.util.Base64.getEncoder().encodeToString((username + ":" + password).getBytes()))
                        .get();
            case COOKIE:
                return Jsoup.connect(url).timeout(dataFetchTime).userAgent("Mozilla/5.0")
                        .cookie("JSESSIONID", authToken).get();
            case TOKEN:
                return Jsoup.connect(url).timeout(dataFetchTime).userAgent("Mozilla/5.0")
                        .header("Authorization", "Bearer " + authToken).get();
            case NONE:
            default:
                return Jsoup.connect(url).timeout(dataFetchTime).userAgent("Mozilla/5.0").get();
        }
    }

    private String fetchStringWithAuth(String url) throws IOException {
        switch (authType) {
            case BASIC:
                return Jsoup.connect(url).timeout(dataFetchTime).userAgent("Mozilla/5.0").header("Authorization",
                        "Basic " + java.util.Base64.getEncoder().encodeToString((username + ":" + password).getBytes()))
                        .method(Connection.Method.GET).ignoreContentType(true).execute().body();
            case COOKIE:
                return Jsoup.connect(url).timeout(dataFetchTime).userAgent("Mozilla/5.0")
                        .cookie("JSESSIONID", authToken).method(Connection.Method.GET).ignoreContentType(true).execute()
                        .body();
            case TOKEN:
                return Jsoup.connect(url).timeout(dataFetchTime).userAgent("Mozilla/5.0")
                        .header("Authorization", "Bearer " + authToken).method(Connection.Method.GET)
                        .ignoreContentType(true).execute().body();
            case NONE:
            default:
                return Jsoup.connect(url).timeout(dataFetchTime).userAgent("Mozilla/5.0").method(Connection.Method.GET)
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
        String pageId = extractParamFromUrl(baseUrl, "pageId");
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

    private String extractParamFromUrl(String url, String paramName) {
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
                    if (pair.startsWith(paramName + "=")) {
                        return pair.substring(paramName.length() + 1);
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