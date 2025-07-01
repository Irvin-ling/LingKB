package com.ling.lingkb.data.extractor;

import com.hankcs.hanlp.mining.word2vec.DocVectorModel;
import com.hankcs.hanlp.mining.word2vec.WordVectorModel;
import com.ling.lingkb.entity.FeatureExtractResult;
import java.io.IOException;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Text Vectorization Utility
 * <p>
 * 1. Text vectorization modeling (Word2Vec/Doc2Vec embeddings)
 * 2. Semantic similarity matrix computation (supports cosine similarity/Euclidean distance metrics)
 * <p>
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2023-07-01
 */
@Slf4j
@Component
@Data
@EqualsAndHashCode(callSuper = false)
@ConfigurationProperties(prefix = "data.extractor.vector")
public class VectorExtractor extends AbstractFeatureExtractor {
    private boolean enable = true;


    @Override
    void doExtract(FeatureExtractResult input) throws IOException {
        log.info("VectorExtractor.doExtract()...");
        if (enable) {

        }
    }


}