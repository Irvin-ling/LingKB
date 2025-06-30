package com.ling.lingkb.entity;

import com.ling.lingkb.util.language.LanguageUtil;
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
public class TextProcessResult extends DocumentParseResult {

    @CodeHint
    public TextProcessResult(DocumentParseResult parentResult) {
        if (parentResult == null) {
            throw new IllegalArgumentException("Parent Result cannot be null");
        }
        this.text = parentResult.text;
        this.metadata = parentResult.metadata;
        // Detect and set the language format of data
        this.language = LanguageUtil.detectLanguage(this.text);
    }

    String processedText = null;
    Language language = Language.UNKNOWN;
}
