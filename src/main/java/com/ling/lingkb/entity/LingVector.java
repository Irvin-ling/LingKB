package com.ling.lingkb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/7/16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LingVector {
    private int id;
    private String workspace;
    private Integer nodeId;
    private String txt;
    private String vector;
    private boolean persisted;
}
