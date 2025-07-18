package com.ling.lingkb.llm.data.processor;

import com.ling.lingkb.entity.CodeHint;
import com.ling.lingkb.entity.LingDocument;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The implementation entry for feature engineering: processing phase
 * <p>
 * When an error occurs during processing, the logic will not continue.
 * Instead, the original text will be returned, so no exception will be thrown.
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/25
 */
@Slf4j
@Component
public class TextProcessorFactory {

    private TextProcessor textProcessor;

    @Autowired
    public TextProcessorFactory(PrimaryProcessor primaryProcessor, LanguageProcessor languageProcessor,
                                SynonymProcessor synonymProcessor, NoiseProcessor noiseProcessor,
                                StructureProcessor structureProcessor, FormatProcessor formatProcessor) {
        textProcessor = primaryProcessor.setNext(languageProcessor).setNext(synonymProcessor).setNext(noiseProcessor)
                .setNext(structureProcessor).setNext(formatProcessor);
    }

    @CodeHint
    public LingDocument process(LingDocument document) {
        String processedText = textProcessor.process(document.getText());
        document.setText(processedText);
        log.debug("Successfully completed the processing phase of text.");
        return document;
    }

    @CodeHint
    public List<LingDocument> batchProcess(List<LingDocument> documents) {
        return documents.stream().map(this::process).collect(Collectors.toList());
    }
}
