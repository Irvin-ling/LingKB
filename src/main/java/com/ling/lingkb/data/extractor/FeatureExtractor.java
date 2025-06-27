package com.ling.lingkb.data.extractor;


import com.ling.lingkb.entity.FeatureExtractResult;

/**
 * The {@code FeatureExtractor} interface defines a contract for components that participate in
 * a feature extraction process, typically used in a pipeline design pattern for data preprocessing.
 * <p>
 * Implementations of this interface are responsible for performing a specific feature extraction
 * operation (such as extracting keywords, generating embeddings, calculating statistical features, etc.)
 * and can pass the processed data to the next extractor in the pipeline if needed.
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/24
 */
public interface FeatureExtractor {
    /**
     * Extracts features from the provided result object and embeds them into the same object.
     * This method mutates the input object by:
     * 1. Reading raw text content from {@code input.getProcessedText()}
     * 2. Computing features (e.g., keywords, numerical metrics, semantic vectors)
     * 3. Setting extracted features into {@code input} via its builder methods
     *
     * @param input the result object to both read input text from and write extracted features to.
     *              Must not be null and should have preprocessed text content available.
     */
    void extract(FeatureExtractResult input);

    /**
     * Sets the next {@code FeatureExtractor} in the processing pipeline. This enables constructing a sequence
     * of feature extraction operations where each extractor can pass the processed data to the next one after
     * performing its own feature extraction step.
     *
     * @param next the subsequent {@code FeatureExtractor} to pass data to. Can be {@code null}
     *             if there are no further extractors in the pipeline.
     * @return the current {@code FeatureExtractor} instance (for method chaining convenience).
     */
    FeatureExtractor setNext(FeatureExtractor next);

}
