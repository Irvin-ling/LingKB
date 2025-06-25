package com.ling.lingkb.data.processor;

/**
 * You can customize some cleaning and filtering rules here. The CustomizedProcessor will execute them fixedly.
 */
public class CustomizedProcessor extends AbstractProcessor {
    private boolean enable = true;

    @Override
    String doClean(String text) {
        //TODO
        return text;
    }
}
