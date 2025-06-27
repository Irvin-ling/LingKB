package com.ling.lingkb.data.processor;


import org.apache.commons.lang3.StringUtils;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/24
 */
public abstract class AbstractTextProcessor implements TextProcessor {
    private TextProcessor next;

    @Override
    public TextProcessor setNext(TextProcessor next) {
        this.next = next;
        return next;
    }

    @Override
    public String process(String text) {
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
