package com.ling.lingkb.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ling.lingkb.entity.ChatTag;
import com.ling.lingkb.entity.LingDocumentLink;
import com.ling.lingkb.llm.client.EmbeddingClient;
import com.ling.lingkb.llm.client.QwenClient;
import com.ling.lingkb.llm.client.QwenPromptHelper;
import com.ling.lingkb.llm.client.VectorStoreClient;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
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
        ChatTag chatTag = json.getObject("chatTag", ChatTag.class);
        if (chatTag.zh2En()) {
            QwenPromptHelper.buildToEnPrompt(json, question);
            qwenClient.fetchStreamData(json, response, null);
        } else if (chatTag.en2Zh()) {
            QwenPromptHelper.buildToZhPrompt(json, question);
            qwenClient.fetchStreamData(json, response, null);
        } else {
            float[] query = embeddingClient.getEmbedding(question);
            List<String> vectorResults = vectorStoreClient.searchTopK(query);
            LingDocumentLink link = vectorStoreClient.searchLink(query);
            QwenPromptHelper.buildPrompt(json, question, vectorResults);
            qwenClient.fetchStreamData(json, response, link);
        }
    }
}
