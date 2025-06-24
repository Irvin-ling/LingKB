package com.ling.lingkb.data.clean;


/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/24
 */
public interface TextCleaner {

    String clean(String text);

    TextCleaner setNext(TextCleaner next);
}
