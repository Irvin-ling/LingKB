package com.ling.lingkb.data.clean;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Multilingual Text Optimizer
 * Handles both English and Chinese text for:
 * 1. Synonym replacement
 * 2. Entity standardization
 * 3. Term unification
 * 4. Abbreviation expansion
 * <p>
 * Modify the /resource/term_mapping.json file for mapping configuration.
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/24
 */
@Slf4j
@Component
@Data
@EqualsAndHashCode(callSuper = false)
@ConfigurationProperties(prefix = "data.clean.optimize")
public class ContentOptimizer extends AbstractTextCleaner {
    private boolean enableOptimizer = false;
    private static Map<String, String> termMapping = new ConcurrentHashMap<>();
    private static Map<String, Pattern> patternCache = new ConcurrentHashMap<>();
    private static List<String> sortedTerms;

    @Override
    protected String doClean(String text) {
        log.info("Multilingual text optimization...");
        if (!enableOptimizer || StringUtils.isBlank(text)) {
            return text;
        }
        if (termMapping.isEmpty()) {
            log.info("initialize semantic mapping structure.");
            try {
                InputStream inputStream = new ClassPathResource("term_mapping.json").getInputStream();
                String configString = IOUtils.toString(inputStream, "UTF-8");
                buildMapping(JSON.parseObject(configString));
            } catch (IOException e) {
                log.error("error term_mapping.json:", e);
                e.printStackTrace();
            }

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