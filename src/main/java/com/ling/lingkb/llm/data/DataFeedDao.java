package com.ling.lingkb.llm.data;

import com.hankcs.hanlp.utility.SentencesUtil;
import com.ling.lingkb.entity.LingDocument;
import com.ling.lingkb.global.SoleMapper;
import com.ling.lingkb.llm.client.EmbeddingClient;
import com.ling.lingkb.llm.client.VectorStoreClient;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Work dao
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2025/7/17
 */
@Component
public class DataFeedDao {
    @Value("${qwen.embedding.chunk.size}")
    private int qwenEmbeddingChunkSize;

    private EmbeddingClient embeddingClient;
    private VectorStoreClient vectorStoreClient;
    @Resource
    private SoleMapper soleMapper;

    @Autowired
    public DataFeedDao(EmbeddingClient embeddingClient, VectorStoreClient vectorStoreClient) {
        this.embeddingClient = embeddingClient;
        this.vectorStoreClient = vectorStoreClient;
    }


    @Async
    public void feed(LingDocument lingDocument) {
        List<String> sentences = SentencesUtil.toSentenceList(lingDocument.getText(), false);
        List<List<String>> sentenceChunks = splitIntoChunks(sentences);
        for (List<String> sentenceChunk : sentenceChunks) {
            List<float[]> embeddingList = embeddingClient.getEmbeddings(sentenceChunk);
            vectorStoreClient.addVectors(lingDocument.getDocId(), sentenceChunk, embeddingList);
        }
    }

    private List<List<String>> splitIntoChunks(List<String> sentences) {
        List<List<String>> chunks = new ArrayList<>();
        if (sentences == null || sentences.isEmpty()) {
            return chunks;
        }

        int totalSize = sentences.size();
        for (int i = 0; i < totalSize; i += qwenEmbeddingChunkSize) {
            int end = Math.min(i + qwenEmbeddingChunkSize, totalSize);
            List<String> chunk = sentences.subList(i, end);
            chunks.add(chunk);
        }
        return chunks;
    }
}
