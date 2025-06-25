package com.ling.lingkb.common.entity;

import java.util.Arrays;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/25
 */
public enum Language {
    /**
     * chinese
     */
    ZH("中文", "zh"),
    /**
     * english
     */
    EN("英文", "en"),
    /**
     * unknown
     */
    UNKNOWN("未知", "unknown");

    private final String displayName;
    private final String isoCode;

    Language(String displayName, String isoCode) {
        this.displayName = displayName;
        this.isoCode = isoCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIsoCode() {
        return isoCode;
    }

    public static Language safeValueOf(String isoCode) {
        return Arrays.stream(values()).filter(language -> language.isoCode.equalsIgnoreCase(isoCode)).findFirst()
                .orElse(UNKNOWN);
    }
}
