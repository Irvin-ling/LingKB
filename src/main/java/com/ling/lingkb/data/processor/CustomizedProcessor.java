package com.ling.lingkb.data.processor;

/**
 * You can customize some cleaning and filtering rules here. The CustomizedProcessor will execute them fixedly.
 */
public class CustomizedProcessor extends AbstractTextProcessor {
    private boolean enable = true;

    @Override
    String doProcess(String text) {
        //TODO
        return text;
    }
}
