package com.ling.lingkb.llm.data.processor;

import com.ling.lingkb.entity.CodeHint;
import com.ling.lingkb.util.LanguageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * synonym Optimizer
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/24
 */
@Slf4j
@Component
public class SynonymProcessor extends AbstractTextProcessor {
    @Value("${data.processor.synonym.enable}")
    private boolean dataSynonymEnable;

    @CodeHint
    @Override
    String doProcess(String text) {
        log.info("SynonymProcessor.doProcess()...");
        if (dataSynonymEnable) {
            return LanguageUtil.synonymRewrite(text);
        }

        return text;
    }
}