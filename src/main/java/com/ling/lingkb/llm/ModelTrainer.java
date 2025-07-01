package com.ling.lingkb.llm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.hankcs.hanlp.classification.classifiers.IClassifier;
import com.hankcs.hanlp.classification.classifiers.NaiveBayesClassifier;
import com.ling.lingkb.entity.Language;
import com.ling.lingkb.util.ResourceUtil;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.classify.LinearClassifierFactory;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Data model trainer
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/30
 */
@Component
@ConfigurationProperties(prefix = "env")
public class ModelTrainer implements CommandLineRunner {
    private Language lang = Language.ZH;
    public static JSONObject synonymMappings;
    public static final IClassifier CLASSIFIER_ZH = new NaiveBayesClassifier();
    public static LinearClassifier<String, String> CLASSIFIER_EN;

    @Override
    public void run(String... args) throws Exception {
        synonymMappings = getSynonymCorpus();

        if (lang == Language.ZH) {
            Map<String, String[]> classifierModel = getClassifierCorpus();
            CLASSIFIER_ZH.train(classifierModel);
        } else {
            CLASSIFIER_EN = LinearClassifierFactory.loadFromFilename("classifier_data.json");
        }
    }

    public void reTrain(ModelType modelType, String modelData) throws IOException {
        switch (modelType) {
            case SYNONYM: {
                synonymMappings = getSynonymCorpus(modelData);
                break;
            }
            case CLASSIFIER: {
                if (lang == Language.ZH) {
                    Map<String, String[]> classifierModel = getClassifierCorpus(modelData);
                    CLASSIFIER_ZH.train(classifierModel);
                } else {
                    File tempFile = File.createTempFile("temp", ".json");
                    CLASSIFIER_EN = LinearClassifierFactory.loadFromFilename(tempFile.getName());
                    tempFile.deleteOnExit();
                }
                break;
            }
            default: {
            }
        }
    }

    private JSONObject getSynonymCorpus() {
        String corpus = ResourceUtil.getResource("term_mapping.json");
        return getSynonymCorpus(corpus);
    }

    private JSONObject getSynonymCorpus(String modelData) {
        return JSON.parseObject(modelData);
    }

    private Map<String, String[]> getClassifierCorpus() {
        String corpus = ResourceUtil.getResource("classifier_data.json");
        return getClassifierCorpus(corpus);
    }

    private Map<String, String[]> getClassifierCorpus(String modelData) {
        return JSON.parseObject(modelData, new TypeReference<>() {
        });
    }

    /**
     * different training types
     */
    enum ModelType {
        /**
         * synonym
         */
        SYNONYM,
        /**
         * classifier
         */
        CLASSIFIER
    }
}
