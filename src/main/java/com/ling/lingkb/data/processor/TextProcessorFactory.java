package com.ling.lingkb.data.processor;

import com.ling.lingkb.common.entity.CodeHint;
import com.ling.lingkb.common.entity.DocumentParseResult;
import com.ling.lingkb.common.entity.FeatureEngineeringResult;
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

    private PrimaryProcessor primaryProcessor;
    private LanguageProcessor languageProcessor;
    private SynonymProcessor synonymProcessor;
    private NoiseProcessor noiseProcessor;
    private StructureProcessor structureProcessor;
    private FormatProcessor formatProcessor;

    @Autowired
    public TextProcessorFactory(PrimaryProcessor primaryProcessor, LanguageProcessor languageProcessor,
                                SynonymProcessor synonymProcessor, NoiseProcessor noiseProcessor,
                                StructureProcessor structureProcessor, FormatProcessor formatProcessor) {
        this.primaryProcessor = primaryProcessor;
        this.languageProcessor = languageProcessor;
        this.synonymProcessor = synonymProcessor;
        this.noiseProcessor = noiseProcessor;
        this.structureProcessor = structureProcessor;
        this.formatProcessor = formatProcessor;
    }

    @CodeHint
    public FeatureEngineeringResult process(DocumentParseResult parseResult) {
        FeatureEngineeringResult engineeringResult = new FeatureEngineeringResult(parseResult);
        TextProcessor textProcessor =
                primaryProcessor.setNext(languageProcessor).setNext(synonymProcessor).setNext(noiseProcessor)
                        .setNext(structureProcessor).setNext(formatProcessor);
        textProcessor.process(engineeringResult);
        log.debug("Successfully completed the processing phase of feature engineering.");
        return engineeringResult;
    }
}
