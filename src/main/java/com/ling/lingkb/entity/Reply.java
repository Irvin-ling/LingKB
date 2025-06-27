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
 * @author shipotian
 * @since 2025/6/19
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reply<T> {
    /**
     * Return code: 200 success, 500 error
     */
    private int code;

    private String message;

    private T data;

    private boolean success;

    public static Reply success(Object data) {
        return Reply.builder().code(200).message("success").success(true).data(data).build();
    }

    public static Reply success() {
        return Reply.builder().code(200).message("success").success(true).build();
    }

    public static Reply failure(String msg) {
        return Reply.builder().code(500).message(msg).build();
    }

    public static Reply failure() {
        return Reply.builder().code(500).message("Request exception occurred").build();
    }
}
