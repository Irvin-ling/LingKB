package com.ling.lingkb.llm.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/7/15
 */
@Slf4j
@Component
public class EmbeddingClient {
    @Value("${qwen.embedding.url}")
    private String qwenEmbeddingUrl;

    private RestTemplate restTemplate;

    public EmbeddingClient() {
        this.restTemplate = new RestTemplate();
    }

    public float[] getEmbedding(String text) {
        log.info("convert to vector：{}", text);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> requestBody = new HashMap<>(1);
        requestBody.put("input", text);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> responseEntity =
                restTemplate.postForEntity(qwenEmbeddingUrl, request, String.class);
        String responseBody = responseEntity.getBody();
        if (responseBody == null || responseBody.isEmpty()) {
            return new float[0];
        }

        JSONObject jsonObject = JSON.parseObject(responseBody);
        JSONArray dataArray = jsonObject.getJSONArray("data");

        if (dataArray == null || dataArray.isEmpty()) {
            return new float[0];
        }

        JSONObject firstDataObj = dataArray.getJSONObject(0);
        if (firstDataObj == null) {
            return new float[0];
        }

        JSONArray embeddingJson = firstDataObj.getJSONArray("embedding");
        if (embeddingJson == null) {
            return new float[0];
        }

        int size = embeddingJson.size();
        float[] embeddingArr = new float[size];

        for (int i = 0; i < size; i++) {
            embeddingArr[i] = embeddingJson.getFloatValue(i);
        }
        return embeddingArr;
    }

    public List<float[]> getEmbeddings(List<String> textList) {
        log.info("convert to vector：{}", String.join("+", textList));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, List<String>> requestBody = new HashMap<>(1);
        requestBody.put("input", textList);
        HttpEntity<Map<String, List<String>>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> responseEntity =
                restTemplate.postForEntity(qwenEmbeddingUrl, request, String.class);
        String responseBody = responseEntity.getBody();
        if (responseBody == null || responseBody.isEmpty()) {
            return new ArrayList<>();
        }

        JSONObject jsonObject = JSON.parseObject(responseBody);
        JSONArray dataArray = jsonObject.getJSONArray("data");

        if (dataArray == null || dataArray.isEmpty()) {
            return new ArrayList<>();
        }
        List<float[]> embeddingArrList = new ArrayList<>();
        for (int i = 0; i < dataArray.size(); i++) {
            JSONObject dataObj = dataArray.getJSONObject(i);
            JSONArray embeddingJson = dataObj.getJSONArray("embedding");
            if (embeddingJson == null) {
                embeddingArrList.add(new float[0]);
            } else {
                float[] embeddingArr = new float[embeddingJson.size()];
                for (int j = 0; j < embeddingJson.size(); j++) {
                    embeddingArr[j] = embeddingJson.getFloatValue(j);
                }
                embeddingArrList.add(embeddingArr);
            }
        }
        return embeddingArrList;
    }
}