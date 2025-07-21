package com.ling.lingkb.llm.data.processor;


import org.apache.commons.lang3.StringUtils;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/24
 */
public abstract class AbstractTextProcessor {

    private AbstractTextProcessor next;

    /**
     * Sets the next {@code TextProcessor} in the chain. This allows for constructing a sequence
     * of text cleaning operations where each processor can pass the text to the next one after
     * performing its own cleaning step.
     *
     * @param next the subsequent {@code TextProcessor} to delegate to. Can be {@code null}
     *             if there is no further processor in the chain.
     * @return the current {@code TextProcessor} instance (for method chaining convenience).
     */
    AbstractTextProcessor setNext(AbstractTextProcessor next) {
        if (this.next == null) {
            this.next = next;
        } else {
            this.next.setNext(next);
        }
        return this;
    }

    /**
     * Performs a text cleaning operation on the text content within.
     * The exact nature of the cleaning (e.g., removing watermarks, normalizing headings)
     * is defined by the implementing class.
     *
     * @param text the input data to be process.
     * @return the output text has be processed.
     */
    String process(String text) {
        if (StringUtils.isNotBlank(text)) {
            text = doProcess(text);
            if (next != null) {
                text = next.process(text);
            }
        }
        return text;
    }

    /**
     * Execute the core processing flow for data cleaning
     *
     * @param text the raw data to be processed
     * @return the processed data
     */
    abstract String doProcess(String text);
}
