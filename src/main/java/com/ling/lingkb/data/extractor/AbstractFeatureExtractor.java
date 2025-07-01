package com.ling.lingkb.data.extractor;


import com.ling.lingkb.entity.FeatureExtractResult;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/24
 */
public abstract class AbstractFeatureExtractor implements FeatureExtractor {
    private FeatureExtractor next;

    @Override
    public FeatureExtractor setNext(FeatureExtractor next) {
        this.next = next;
        return next;
    }

    @Override
    public void extract(FeatureExtractResult input) {
        if (input != null && StringUtils.isNotBlank(input.getProcessedText())) {
            doExtract(input);
            if (next != null) {
                next.extract(input);
            }
        }
    }

    /**
     * Execute the core processing flow for feature extracting
     *
     * @param input the result object to both read input text from and write extracted features to.
     */
    abstract void doExtract(FeatureExtractResult input) throws IOException;
}
