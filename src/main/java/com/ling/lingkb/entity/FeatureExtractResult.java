package com.ling.lingkb.entity;
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

import java.util.ArrayList;
import java.util.HashMap;
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
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class FeatureExtractResult extends TextProcessResult {

    @CodeHint
    public FeatureExtractResult(TextProcessResult parentResult) {
        if (parentResult == null) {
            throw new IllegalArgumentException("Parent Result cannot be null");
        }
        this.text = parentResult.text;
        this.metadata = parentResult.metadata;
        this.processedText = parentResult.processedText;
        this.language = parentResult.language;
    }

    private List<String> stopWordsRemoved = new ArrayList<>();
    private List<String> stemmedOrLemmatized = new ArrayList<>();
    private Map<String, Integer> metricCount = new HashMap<>();
    private Map<String, Integer> termFrequency = new HashMap<>();
    private List<String> sentences = new ArrayList<>();
    private List<String> paragraphs = new ArrayList<>();
    private List<String> summaries = new ArrayList<>();
    private Map<String, String> tocMap = new HashMap<>();
    private List<String> keywords = new ArrayList<>();
    private List<String> topics = new ArrayList<>();
    private String category;

    private Map<String, List<String>> namedEntities = new HashMap<>();
    private Map<String, List<String>> posTags = new HashMap<>();

    private double[] textVector = new double[]{};
    private Map<String, Double> similarityMatrix = new HashMap<>();
    private int sentimentPolarity;

    public boolean isChinese() {
        return Language.ZH == this.language;
    }

    public boolean isEnglish() {
        return Language.EN == this.language;
    }
}
