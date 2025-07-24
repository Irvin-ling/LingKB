package com.ling.lingkb.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * External link to the data
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2025/7/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LingDocumentLink {
    private int id;
    private String docId;
    private String workspace;
    /**
     * 0-image|1-code|2-table|3-webLink
     */
    private int type;
    /**
     * base64Image|code|List<List<String>|webUrl
     */
    private String content;
    /**
     * imageUrl|language|columnsCount,rowsCount|webText=desc
     */
    private String contentAssistant;
    private String descText;
    private String descVector;
}
