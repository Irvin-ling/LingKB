package com.ling.lingkb.data.processor;


import com.ling.lingkb.common.entity.FeatureEngineeringResult;
import org.apache.commons.lang3.StringUtils;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/24
 */
public abstract class AbstractProcessor implements TextProcessor {
    private TextProcessor next;

    @Override
    public TextProcessor setNext(TextProcessor next) {
        this.next = next;
        return next;
    }

    @Override
    public void process(FeatureEngineeringResult input) {
        String text = input.getCleanedTextContent();
        if (StringUtils.isNotBlank(text)) {
            text = doClean(text);
            input.setCleanedTextContent(text);
            if (next != null) {
                next.process(input);
            }
        }
    }

    /**
     * Execute the core processing flow for data cleaning
     *
     * @param text the raw data to be processed
     * @return the cleaned data
     */
    abstract String doClean(String text);
}
