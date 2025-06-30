package com.ling.lingkb.entity;
/*
 * ------------------------------------------------------------------
 * Copyright @ 2025 Hangzhou Ling Technology Co.,Ltd. All rights reserved.
 * ------------------------------------------------------------------
 * Product: LingKB
 * Module Name: LingKB
 * Date Created: 2025/6/19
 * Description:
 * ------------------------------------------------------------------
 * Modification History
 * DATE            Name           Description
 * ------------------------------------------------------------------
 * 2025/6/19       spt
 * ------------------------------------------------------------------
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data reception for the data parsing phase
 *
 * @author shipotian
 * @since 2025/6/19
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentParseResult {
    /**
     * Document Text
     */
    String text;

    /**
     * Document Metadata
     */
    DocumentMetadata metadata = new DocumentMetadata();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentMetadata {
        private String author;
        private String sourceFileName;
        private long creationDate;
        private int pageCount;
    }
}
