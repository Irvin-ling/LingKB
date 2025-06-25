package com.ling.lingkb.common.entity;
/*
 * ------------------------------------------------------------------
 * Copyright @ 2025 Hangzhou Ling Technology Co.,Ltd. All rights reserved.
 * ------------------------------------------------------------------
 * Product: LingKB
 * Module Name: LingKB
 * Date Created: 2025/6/25
 * Description:
 * ------------------------------------------------------------------
 * Modification History
 * DATE            Name           Description
 * ------------------------------------------------------------------
 * 2025/6/25       spt
 * ------------------------------------------------------------------
 */

import com.ling.lingkb.util.language.LanguageUtil;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Data reception for the data processing and data feature extraction phases
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class FeatureEngineeringResult extends DocumentParseResult {

    @CodeHint
    public FeatureEngineeringResult(DocumentParseResult parentResult) {
        if (parentResult == null) {
            throw new IllegalArgumentException("Parent Result cannot be null");
        }
        this.textContent = parentResult.textContent;
        this.metadata = parentResult.metadata;
        this.cleanedTextContent = this.textContent;
        this.language = LanguageUtil.detectLanguage(this.textContent);
        // Detect and set the language format of data
        this.textLength = this.textContent.length();
    }

    private String cleanedTextContent;
    private Language language;
    private int textLength;

    private List<String> stopWordsRemoved;
    private List<String> stemmedOrLemmatized;
    private Map<String, List<String>> namedEntities;
    private Map<String, Integer> termFrequency;
    private Map<String, List<String>> posTags;
    private List<String> keywords;
    private List<String> topics;
    private double[] textVector;
    private Map<String, Double> similarityMatrix;
    private List<String> sentences;
    private List<String> paragraphs;
    private int sentimentPolarity;
}
