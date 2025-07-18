package com.ling.lingkb.llm.client;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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

    public QwenClient() {
        this.restTemplate = new RestTemplate();
        noThinkMessage = new JSONObject();
        noThinkMessage.put("role", "system");
        noThinkMessage.put("content", "no_think");
    }

    public void fetchStreamData(JSONObject requestJson, HttpServletResponse response) {
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        restTemplate.execute(qwenUrl, HttpMethod.POST, request -> {
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
        }, responseExtractor -> {
            try (InputStream is = responseExtractor.getBody();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

                PrintWriter writer = response.getWriter();
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
            } catch (IOException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter writer = response.getWriter()) {
                    writer.write("data: {\"error\":\"stream interrupted\"}\n\n");
                    writer.flush();
                }
                log.error("Stream error", e);
            }
            return null;
        });
    }
}
