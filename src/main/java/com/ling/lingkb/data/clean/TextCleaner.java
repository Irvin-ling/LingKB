package com.ling.lingkb.data.clean;


/**
 * The {@code TextCleaner} interface defines a contract for components that participate in
 * a text cleaning process, typically used in a chain-of-responsibility design pattern.
 * <p>
 * Implementations of this interface are responsible for performing a specific text cleaning
 * operation (such as removing headers/footers, sanitizing sensitive info, etc.) and can
 * delegate to the next cleaner in the chain if needed.
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/24
 */
public interface TextCleaner {

    /**
     * Performs a text cleaning operation on the given input text.
     * The exact nature of the cleaning (e.g., removing watermarks, normalizing headings)
     * is defined by the implementing class.
     *
     * @param text the input text to be cleaned. Must not be {@code null} (implementations
     *             should handle or document null cases as needed).
     * @return the cleaned text after applying the specific cleaning operation.
     */
    String clean(String text);

    /**
     * Sets the next {@code TextCleaner} in the chain. This allows for constructing a sequence
     * of text cleaning operations where each cleaner can pass the text to the next one after
     * performing its own cleaning step.
     *
     * @param next the subsequent {@code TextCleaner} to delegate to. Can be {@code null}
     *             if there is no further cleaner in the chain.
     * @return the current {@code TextCleaner} instance (for method chaining convenience).
     */
    TextCleaner setNext(TextCleaner next);
}
