package com.ling.lingkb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/7/25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatTag {
    private String translation;

    public boolean zh2En() {
        return StringUtils.equalsAnyIgnoreCase(translation, "zh2En");
    }

    public boolean en2Zh() {
        return StringUtils.equalsAnyIgnoreCase(translation, "en2Zh");
    }
}
