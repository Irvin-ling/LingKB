package com.ling.lingkb.util.language;
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

import com.ling.lingkb.common.entity.Language;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.PropertiesUtils;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/25
 */
class ChineseUtil {
    private final static Language LANG = Language.ZH;
    private static StanfordCoreNLP pipeline = new StanfordCoreNLP(
            PropertiesUtils.asProperties("annotators", "tokenize,ssplit,pos", "tokenize.language", LANG.getIsoCode()));

    static boolean isChinese(Language lang) {
        return LANG == lang;
    }

    private static List<CoreLabel> getTokens(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        Annotation document = new Annotation(text);
        pipeline.annotate(document);
        return document.get(CoreAnnotations.TokensAnnotation.class);
    }

    static List<String> tokenize(String text) {
        return getTokens(text).stream().map(CoreLabel::word).collect(Collectors.toList());
    }
}
