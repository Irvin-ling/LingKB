package com.ling.lingkb.data.processor;

import lombok.extern.slf4j.Slf4j;

/**
 * You can customize some cleaning and filtering rules here. The CustomizedProcessor will execute them fixedly.
 */
@Slf4j
public class CustomizedProcessor extends AbstractTextProcessor {
    private boolean enable = true;

    @Override
    String doProcess(String text) {
        //TODO
        log.info("CustomizedProcessor.doProcess()...");
        return text;
    }
}
