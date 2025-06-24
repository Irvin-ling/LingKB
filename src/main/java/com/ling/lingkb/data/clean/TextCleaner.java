package com.ling.lingkb.data.clean;
/*
 * ------------------------------------------------------------------
 * Copyright @ 2025 Hangzhou Ling Technology Co.,Ltd. All rights reserved.
 * ------------------------------------------------------------------
 * Product: LingKB
 * Module Name: LingKB
 * Date Created: 2025/6/24
 * Description:
 * ------------------------------------------------------------------
 * Modification History
 * DATE            Name           Description
 * ------------------------------------------------------------------
 * 2025/6/24       spt
 * ------------------------------------------------------------------
 */

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/24
 */
public interface TextCleaner {

    String clean(String text);

    TextCleaner setNext(TextCleaner next);
}
