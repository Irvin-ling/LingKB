package com.ling.lingkb.common.exception;
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

/**
 * 不支持的文档类型异常
 *
 * @author shipotian
 * @date 2025/6/19
 * @since 1.0.0
 */
public class UnsupportedDocumentTypeException extends DocumentParseException {
    public UnsupportedDocumentTypeException(String message) {
        super(message);
    }
}
