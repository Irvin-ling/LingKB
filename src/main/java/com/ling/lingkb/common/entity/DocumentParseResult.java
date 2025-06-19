package com.ling.lingkb.common.entity;
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

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author shipotian
 * @date 2025/6/19
 * @since 1.0.0
 */
@Data
public class DocumentParseResult {
    /**
     * 文本内容
     */
    private String textContent;
    /**
     * 源文件名
     */
    private String sourceFileName;
    /**
     * 元数据（作者、创建时间等）
     */
    @Builder.Default
    private DocumentMetadata metadata = new DocumentMetadata();

    @Data
    @Builder
    @NoArgsConstructor
    public class DocumentMetadata {
        private String author;
        private String creationDate;
        private String pageCount;
    }
}
