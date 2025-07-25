package com.ling.lingkb.llm.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ling.lingkb.entity.LingDocumentLink;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/7/9
 */
@Slf4j
@Component
public class QwenClient {

    @Value("${qwen.chat.url}")
    private String qwenUrl;
    @Value("${qwen.chat.temperature}")
    private float qwenTemperature;
    @Value("${qwen.chat.think}")
    private boolean qwenThink;

    private RestTemplate restTemplate;
    private JSONObject noThinkMessage;

    @Autowired
    public QwenClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.noThinkMessage = new JSONObject();
        this.noThinkMessage.put("role", "system");
        this.noThinkMessage.put("content", "no_think");
    }

    public void fetchStreamData(JSONObject requestJson, HttpServletResponse response,
                                LingDocumentLink lingDocumentLink) {
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        try {
            restTemplate.execute(qwenUrl, HttpMethod.POST,
                    request -> configureRequest(requestJson, request),
                    responseExtractor -> {
                        try {
                            writeResponse(response, lingDocumentLink, responseExtractor);
                        } finally {
                            try {
                                responseExtractor.close();
                            } catch (Exception e) {
                                log.warn("Error closing response", e);
                            }
                        }
                        return null;
                    });
        } catch (Exception e) {
            log.error("Error in fetchStreamData", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                PrintWriter writer = response.getWriter();
                writer.write("data: {\"error\":\"request failed\"}\n\n");
                writer.flush();
            } catch (IOException ex) {
                log.error("Error writing error response", ex);
            }
        }
    }

    private void writeResponse(HttpServletResponse response, LingDocumentLink lingDocumentLink,
                               ClientHttpResponse responseExtractor) throws IOException {
        try (InputStream is = responseExtractor.getBody();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
             PrintWriter writer = response.getWriter()) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    writer.write(line + "\n");
                    writer.flush();
                } else if (!line.isEmpty()) {
                    writer.write("data: {\"error\":\"unexpected line format\"}\n\n");
                    writer.flush();
                    log.warn("Unexpected line: {}", line);
                }
            }
            writer.write("data: [DONE]\n\n");
            writer.flush();

            if (lingDocumentLink != null) {
                writer.write("added: 我也查找了非文本库，下面的" + typeToChinese(lingDocumentLink.getType()) + "可能对你有用：\n");
                writer.flush();
                writer.write("link: " + toLinkJson(lingDocumentLink) + "\n\n");
                writer.flush();
            }
        }
    }

    private void configureRequest(JSONObject requestJson, ClientHttpRequest request) throws IOException {
        request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        request.getHeaders().setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        if (requestJson != null) {
            ObjectMapper mapper = new ObjectMapper();
            requestJson.put("temperature", qwenTemperature);
            requestJson.put("stream", true);
            JSONArray messages = requestJson.getJSONArray("messages");

            if (!qwenThink && !"system".equals(messages.getJSONObject(0).getString("role"))) {
                messages.add(0, noThinkMessage);
                requestJson.put("messages", messages);
            }
            mapper.writeValue(request.getBody(), requestJson);
        }
    }

    private JSONObject toLinkJson(LingDocumentLink lingDocumentLink) {
        JSONObject result = new JSONObject();
        switch (lingDocumentLink.getType()) {
            case 0:
                result.put("type", "image");
                result.put("content", lingDocumentLink.getContent());
                break;
            case 1:
                result.put("type", "code");
                result.put("language", lingDocumentLink.getContentAssistant());
                result.put("content", lingDocumentLink.getContent());
                break;
            case 2:
                result.put("type", "table");
                String[] counts = lingDocumentLink.getContentAssistant().split(",");
                result.put("cols", Integer.parseInt(counts[0]));
                result.put("rows", Integer.parseInt(counts[1]));
                result.put("data",
                        JSON.parseObject(lingDocumentLink.getContent(), new TypeReference<List<List<String>>>() {
                        }));
                break;
            case 3:
                result.put("type", "link");
                result.put("content", lingDocumentLink.getContent());
                result.put("webText", lingDocumentLink.getContentAssistant());
                break;
            default:
                log.warn("Unsupported type: {}", lingDocumentLink.getType());
        }
        return result;
    }

    private String typeToChinese(int linkType) {
        switch (linkType) {
            case 0:
                return "图片";
            case 1:
                return "代码块";
            case 2:
                return "表格";
            case 3:
                return "链接";
            default:
                log.warn("Unsupported type: {}", linkType);
        }
        return "内容";
    }
}