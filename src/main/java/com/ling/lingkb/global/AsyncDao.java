package com.ling.lingkb.global;

import com.hankcs.hanlp.utility.SentencesUtil;
import com.ling.lingkb.entity.LingDocument;
import com.ling.lingkb.entity.LingVector;
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
public class AsyncDao {
    @Value("${qwen.embedding.chunk.size}")
    private int qwenEmbeddingChunkSize;
    @Value("${system.workspace}")
    private String workspace;
    @Resource
    private SoleMapper soleMapper;
    private EmbeddingClient embeddingClient;
    private VectorStoreClient vectorStoreClient;

    @Autowired
    public AsyncDao(EmbeddingClient embeddingClient, VectorStoreClient vectorStoreClient) {
        this.embeddingClient = embeddingClient;
        this.vectorStoreClient = vectorStoreClient;
    }

    @Async
    public void feed(LingDocument lingDocument) {
        List<String> sentences = SentencesUtil.toSentenceList(lingDocument.getText(), false);
        List<List<String>> sentenceChunks = splitIntoChunks(sentences);
        for (List<String> sentenceChunk : sentenceChunks) {
            feedInChunk(lingDocument.getDocId(), sentenceChunk);
        }
        vectorStoreClient.setToInconsistent();
    }

    @Async
    public void feedInChunk(String docId, List<String> texts) {
        List<float[]> vectors = embeddingClient.getEmbeddings(texts);
        List<LingVector> vectorList = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            float[] vector = vectors.get(i);
            LingVector lingVector =
                    LingVector.builder().workspace(workspace).docId(docId).txt(text).vector(floatsToString(vector))
                            .persisted(false).build();
            vectorList.add(lingVector);
        }
        soleMapper.batchSaveVectors(vectorList);
    }

    @Async
    public void removeNode(int nodeId) {
        soleMapper.removeVectorByNodeId(nodeId);
        vectorStoreClient.setToInconsistent();
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

    private static String floatsToString(float[] array) {
        StringBuilder sb = new StringBuilder();
        sb.append(array[0]);
        final String delimiter = ",";
        for (int i = 1; i < array.length; i++) {
            sb.append(delimiter).append(array[i]);
        }
        return sb.toString();
    }
}
