package com.ling.lingkb.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ling.lingkb.llm.client.EmbeddingClient;
import com.ling.lingkb.llm.client.QwenClient;
import com.ling.lingkb.llm.client.VectorStoreClient;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/25
 */
@RestController
@RequestMapping("/ling")
@CrossOrigin(origins = {"http://127.0.0.1:8080", "http://localhost:8080"}, allowCredentials = "true") // TODO to remove
public class DialogController {
    @Value("${vector.window.size}")
    private String vectorWindowSize; //TODO to use

    private QwenClient qwenClient;
    private EmbeddingClient embeddingClient;
    private VectorStoreClient vectorStoreClient;


    @Autowired
    public DialogController(QwenClient qwenClient, EmbeddingClient embeddingClient,
                            VectorStoreClient vectorStoreClient) {
        this.qwenClient = qwenClient;
        this.embeddingClient = embeddingClient;
        this.vectorStoreClient = vectorStoreClient;
    }

    @PostMapping(value = "/dialog", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void dialog(@RequestBody JSONObject json, HttpServletResponse response) {
        JSONArray messages = json.getJSONArray("messages");
        JSONObject lastJson = messages.getJSONObject(messages.size() - 1);
        String question = lastJson.getString("content");
        float[] query = embeddingClient.getEmbedding(question);
        String result = vectorStoreClient.searchTopOne(query);

        //TODO To be optimized
        if (StringUtils.isBlank(result)) {
            result = String.format("以下内容是知识库对话：问题是[%s]，却未搜索到相关答案，根据你自身能力回答并请告知用户-知识库内没有搜索到相关信息，", question);
        } else {
            result = String.format("以下内容是知识库对话：问题是[%s]，搜索到的相关句子是[%s]，从这些句子里选择最合适的并组织语言后返回", question, result);
        }
        lastJson.put("content", result);
        System.out.println(json);
        qwenClient.fetchStreamData(json, response);
    }
}
