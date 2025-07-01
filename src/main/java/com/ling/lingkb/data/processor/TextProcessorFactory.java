package com.ling.lingkb.data.processor;

import com.ling.lingkb.entity.CodeHint;
import com.ling.lingkb.entity.DocumentParseResult;
import com.ling.lingkb.entity.TextProcessResult;
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
    public TextProcessResult process(DocumentParseResult parseResult) {
        TextProcessResult textProcessResult = new TextProcessResult(parseResult);
        String processedText = textProcessor.process(textProcessResult.getText());
        textProcessResult.setProcessedText(processedText);
        log.debug("Successfully completed the processing phase of text.");
        return textProcessResult;
    }

    @CodeHint
    public List<TextProcessResult> batchProcess(List<DocumentParseResult> parseResults) {
        return parseResults.stream().map(this::process).collect(Collectors.toList());
    }
}
