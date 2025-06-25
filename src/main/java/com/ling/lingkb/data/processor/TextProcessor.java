package com.ling.lingkb.data.processor;


import com.ling.lingkb.common.entity.FeatureEngineeringResult;

/**
 * The {@code TextProcessor} interface defines a contract for components that participate in
 * a text cleaning process, typically used in a chain-of-responsibility design pattern.
 * <p>
 * Implementations of this interface are responsible for performing a specific text cleaning
 * operation (such as removing headers/footers, sanitizing sensitive info, etc.) and can
 * delegate to the next processor in the chain if needed.
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/24
 */
public interface TextProcessor {
    /**
     * Performs a text cleaning operation on the text content within.
     * The exact nature of the cleaning (e.g., removing watermarks, normalizing headings)
     * is defined by the implementing class.
     *
     * @param input the input data to be cleaned.
     */
    void process(FeatureEngineeringResult input);

    /**
     * Sets the next {@code TextProcessor} in the chain. This allows for constructing a sequence
     * of text cleaning operations where each processor can pass the text to the next one after
     * performing its own cleaning step.
     *
     * @param next the subsequent {@code TextProcessor} to delegate to. Can be {@code null}
     *             if there is no further processor in the chain.
     * @return the current {@code TextProcessor} instance (for method chaining convenience).
     */
    TextProcessor setNext(TextProcessor next);

}
