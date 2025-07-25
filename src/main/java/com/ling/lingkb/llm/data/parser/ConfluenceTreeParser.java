package com.ling.lingkb.llm.data.parser;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ling.lingkb.entity.LingDocument;
import com.ling.lingkb.entity.LingDocumentLink;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
    @Value("${data.parser.confluence.link}")
    private boolean webLink;
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
    public List<LingDocument> parse(String rootUrl) throws Exception {
        log.info("ConfluenceTreeParser.parse({})...", rootUrl);
        visitedUrls.clear();
        init(rootUrl);
        List<LingDocument> lingDocuments = new ArrayList<>();
        parseRecursive(rootUrl, lingDocuments, 0);
        return lingDocuments;
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

    private void parseRecursive(String url, List<LingDocument> results, int depth) throws IOException {
        if (depth > dataConfluenceDepth || visitedUrls.contains(url)) {
            return;
        }

        visitedUrls.add(url);
        Document doc = fetchDocumentWithAuth(url);
        String title = doc.title();
        LingDocument lingDocument = new LingDocument();
        Element element = findContentArea(doc);
        log.info("successfully resolved: {}", url);
        lingDocument.setAuthor(getAuthor(doc));
        lingDocument.setCreationDate(0);
        lingDocument.setSourceFileName(url);
        lingDocument.setCreationDate(System.currentTimeMillis());
        lingDocument.setPageCount(1);
        linkContentParse(lingDocument, element);
        lingDocument.getLinks().forEach(link -> link.setDescText(title + "\n" + link.getDescText()));
        String currentContent = element.text();
        if (currentContent.length() > dataMaxLength) {
            log.warn("current file-{} content truncated due to size limit {}", doc.title(), dataMaxLength);
            currentContent = currentContent.substring(0, dataMaxLength);
        }
        lingDocument.setText(currentContent);
        lingDocument.setSize(currentContent.getBytes(StandardCharsets.UTF_8).length);
        results.add(lingDocument);
        // Recursive processing of subPages
        if (depth < dataConfluenceDepth) {
            for (String childUrl : findChildPageLinks(url)) {
                parseRecursive(childUrl, results, depth + 1);
            }
        }
    }

    @SneakyThrows
    @Override
    public void linkContentParse(LingDocument lingDocument, Object obj) {
        Element element = (Element) obj;
        List<LingDocumentLink> links = new ArrayList<>();
        extractImages(links, element);
        extractCodes(links, element);
        extractTables(links, element);
        if (webLink) {
            extractWebLinks(lingDocument.getSourceFileName(), links, element);
        }
        lingDocument.setLinks(links);
    }

    private void extractImages(List<LingDocumentLink> links, Element element) throws IOException {
        Elements images = element.select("img");
        for (Element image : images) {
            String imageClass = image.attr("class");
            if (!imageClass.contains("logo")) {
                LingDocumentLink link = new LingDocumentLink();
                link.setType(0);
                String imgDesc = getPreText(image, "img");
                if (StringUtils.isNotBlank(imgDesc)) {
                    link.setDescText(imgDesc);
                    String absoluteUrl = image.absUrl("src");
                    byte[] imageBytes = fetchBytesWithAuth(absoluteUrl);
                    if(imageBytes != null) {
                        link.setContent(addBase64Header(imageBytes));
                        link.setContentAssistant(absoluteUrl);
                        links.add(link);
                    }
                }
            }
            image.remove();
        }
    }

    private String addBase64Header(byte[] imageBytes) {
        String template = "data:image/%s;base64,%s";
        String imageType;
        if (imageBytes[0] == (byte) 0xFF && imageBytes[1] == (byte) 0xD8 && imageBytes[2] == (byte) 0xFF) {
            imageType = "jpeg";
        } else if (imageBytes[0] == (byte) 0x89 && imageBytes[1] == (byte) 0x50 && imageBytes[2] == (byte) 0x4E &&
                imageBytes[3] == (byte) 0x47 && imageBytes[4] == (byte) 0x0D && imageBytes[5] == (byte) 0x0A &&
                imageBytes[6] == (byte) 0x1A && imageBytes[7] == (byte) 0x0A) {
            imageType = "png";
        } else if (imageBytes[0] == (byte) 0x47 && imageBytes[1] == (byte) 0x49 && imageBytes[2] == (byte) 0x46 &&
                imageBytes[3] == (byte) 0x38) {
            imageType = "gif";
        } else if (imageBytes[0] == (byte) 0x42 && imageBytes[1] == (byte) 0x4D) {
            imageType = "bmp";
        } else {
            imageType = "png";
        }
        return String.format(template, imageType, Base64.getEncoder().encodeToString(imageBytes));
    }

    private void extractCodes(List<LingDocumentLink> links, Element element) {
        Elements codeBlocks = element.select("pre[data-syntaxhighlighter-params]");
        for (Element code : codeBlocks) {
            LingDocumentLink link = new LingDocumentLink();
            link.setType(1);
            String codeDesc = getPreText(code, "pre");
            if (StringUtils.isNotBlank(codeDesc)) {
                link.setDescText(codeDesc);
                link.setContent(code.text());
                String params = code.attr("data-syntaxhighlighter-params");
                String language = parseParam(params, "brush");
                link.setContentAssistant(StringUtils.isBlank(language) ? "Plain Text" : language);
                links.add(link);
            }
            code.remove();
        }
    }

    private void extractTables(List<LingDocumentLink> links, Element element) {
        Elements tables = element.select("table");
        for (Element table : tables) {
            List<List<String>> tableData = new ArrayList<>();
            Elements rows = table.select("tr");
            int maxCols = 0;
            for (Element row : rows) {
                List<String> rowData = new ArrayList<>();
                Elements cells = row.select("td, th");
                for (Element cell : cells) {
                    int colspan = getSpanValue(cell, "colspan", 1);
                    String cellText = cell.text().trim();
                    rowData.add(cellText);
                    for (int i = 1; i < colspan; i++) {
                        rowData.add("");
                    }
                }
                if (rowData.size() > maxCols) {
                    maxCols = rowData.size();
                }
                tableData.add(rowData);
            }
            for (List<String> row : tableData) {
                while (row.size() < maxCols) {
                    row.add("");
                }
            }

            LingDocumentLink link = new LingDocumentLink();
            link.setType(2);
            String tableDesc = getPreText(table, "table");
            if (StringUtils.isNotBlank(tableDesc)) {
                link.setContent(JSON.toJSONString(tableData));
                link.setDescText(tableDesc + "\n" + link.getContent());
                link.setContentAssistant(maxCols + "," + rows.size());
                links.add(link);
            }
            table.remove();
        }
    }

    private void extractWebLinks(String rootUrl, List<LingDocumentLink> links, Element element)
            throws MalformedURLException {
        Elements webLinks = element.select("a");
        for (Element webLink : webLinks) {
            LingDocumentLink link = new LingDocumentLink();
            link.setType(3);
            link.setDescText(webLink.text());
            URL url = new URL(rootUrl);
            link.setContent(url.getProtocol() + "://" + url.getHost() + webLink.attr("href"));
            link.setContentAssistant(link.getDescText());
            links.add(link);
            webLink.remove();
        }
    }

    private String getPreText(Element element, String selfTag) {
        String text = null;
        if (!StringUtils.equalsAnyIgnoreCase(element.tagName(), selfTag, "div")) {
            text = element.text().trim();
        }
        if (StringUtils.isBlank(text)) {
            Element preElement = element.previousElementSibling();
            if (preElement == null) {
                Element parent = element.parent();
                if (parent == null) {
                    return null;
                } else {
                    return getPreText(parent, selfTag);
                }
            } else {
                return getPreText(preElement, selfTag);
            }
        }
        return text;
    }

    private static String parseParam(String params, String key) {
        String[] pairs = params.split(";\\s*");
        for (String pair : pairs) {
            String[] kv = pair.split(":\\s*");
            if (kv.length == 2 && kv[0].trim().equals(key)) {
                return kv[1].trim();
            }
        }
        return "";
    }

    private static int getSpanValue(Element cell, String attr, int defaultValue) {
        String spanValue = cell.attr(attr);
        if (!spanValue.isEmpty()) {
            try {
                return Integer.parseInt(spanValue);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private Element findContentArea(Document doc) {
        Element contentArea = doc.selectFirst(".body-content");
        if (contentArea != null) {
            return contentArea;
        }
        return doc.selectFirst(".wiki-content");
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

    private byte[] fetchBytesWithAuth(String url) {
        try {
            switch (authType) {
                case BASIC:
                    return Jsoup.connect(url).timeout(dataFetchTime).ignoreContentType(true).userAgent("Mozilla/5.0")
                            .header("Authorization", "Basic " +
                                    java.util.Base64.getEncoder().encodeToString((username + ":" + password).getBytes()))
                            .execute().bodyAsBytes();
                case COOKIE:
                    return Jsoup.connect(url).timeout(dataFetchTime).ignoreContentType(true).userAgent("Mozilla/5.0")
                            .cookie("JSESSIONID", authToken).execute().bodyAsBytes();
                case TOKEN:
                    return Jsoup.connect(url).timeout(dataFetchTime).ignoreContentType(true).userAgent("Mozilla/5.0")
                            .header("Authorization", "Bearer " + authToken).execute().bodyAsBytes();
                case NONE:
                default:
                    return Jsoup.connect(url).timeout(dataFetchTime).ignoreContentType(true).userAgent("Mozilla/5.0")
                            .execute().bodyAsBytes();
            }
        }catch (Exception e){
            return null;
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