package com.ling.lingkb.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data reception for the data parsing phase
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/19
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LingDocument {
    private String text;
    private String docId;
    private String workspace;
    private String author;
    private long size;
    private String sourceFileName;
    private long creationDate;
    private int pageCount;

    private int charCount;
    private int wordCount;
    private int sentenceCount;
    private String keywords;
    private boolean persisted;
}
