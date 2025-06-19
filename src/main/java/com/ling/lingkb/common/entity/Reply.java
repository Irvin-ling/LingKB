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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author shipotian
 * @date 2025/6/19
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reply<T> {
    /**
     * 返回码：200成功、500错误
     */
    private int code;
    /**
     * 返回信息
     */
    private String message;
    /**
     * 返回数据
     */
    private T data;
    /**
     * 返回状态
     */
    private boolean success;

    public static Reply success(Object data) {
        return Reply.builder().code(200).message("访问成功").success(true).data(data).build();
    }

    public static Reply success() {
        return Reply.builder().code(200).message("访问成功").success(true).build();
    }

    public static Reply failure(String msg) {
        return Reply.builder().code(500).message(msg).build();
    }

    public static Reply failure() {
        return Reply.builder().code(500).message("请求发生异常").build();
    }
}
