package com.ling.lingkb.data.clean;


/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/24
 */
public abstract class AbstractTextCleaner implements TextCleaner {
    private TextCleaner next;

    @Override
    public TextCleaner setNext(TextCleaner next) {
        this.next = next;
        return next;
    }

    @Override
    public String clean(String text) {
        String result = doClean(text);
        return next != null ? next.clean(result) : result;
    }

    protected abstract String doClean(String text);
}
