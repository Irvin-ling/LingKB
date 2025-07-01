package com.ling.lingkb.data.processor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hankcs.hanlp.dictionary.CoreSynonymDictionary;
import com.ling.lingkb.entity.CodeHint;
import com.ling.lingkb.llm.ModelTrainer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Multilingual Text Optimizer
 * Handles both English and Chinese text for:
 * 1. Synonym replacement
 * 2. Entity standardization
 * 3. Term unification
 * 4. Abbreviation expansion
 * <p>
 * You can implement custom term rules by configuring the `/resource/synonym_mapping.json` file.
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/24
 */
@Slf4j
@Component
@Data
@EqualsAndHashCode(callSuper = false)
@ConfigurationProperties(prefix = "data.processor.synonym")
public class SynonymProcessor extends AbstractTextProcessor {
    private boolean enable = false;
    private static Map<String, String> termMapping = new ConcurrentHashMap<>();
    private static Map<String, Pattern> patternCache = new ConcurrentHashMap<>();
    private static List<String> sortedTerms;

    @CodeHint
    @Override
    String doProcess(String text) {
        log.info("SynonymProcessor.doProcess()...");

        if (enable) {
            if (termMapping.isEmpty()) {
                log.info("initialize semantic mapping structure.");
                buildMapping(ModelTrainer.synonymMappings);

                // Sort terms by length and prioritize replacing longer terms.
                sortedTerms = new ArrayList<>(termMapping.keySet());
                sortedTerms.sort((t1, t2) -> t2.length() - t1.length());

                // Precompile regular expressions
                patternCache = new ConcurrentHashMap<>(sortedTerms.size());
                for (String term : sortedTerms) {
                    patternCache.put(term, Pattern.compile("\\b" + Pattern.quote(term) + "\\b"));
                }
            }

            for (String term : sortedTerms) {
                String replacement = termMapping.get(term);
                text = patternCache.get(term).matcher(text).replaceAll(replacement);
            }

            text = CoreSynonymDictionary.rewrite(text);
        }

        return text;
    }

    private void buildMapping(JSONObject configJson) {
        for (Object value : configJson.values()) {
            JSONObject mappings = JSON.parseObject(value.toString());
            for (Map.Entry<String, Object> entry : mappings.entrySet()) {
                String standard = entry.getKey();
                Object values = entry.getValue();

                if (values instanceof List) {
                    @SuppressWarnings("unchecked") List<String> variants = (List<String>) values;
                    for (String variant : variants) {
                        termMapping.put(variant, standard);
                        termMapping.put(standard, standard);
                    }
                } else if (values instanceof String) {
                    termMapping.put(entry.getKey(), (String) values);
                }
            }
        }
    }

}