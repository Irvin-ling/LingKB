package com.ling.lingkb.llm.data.parser;

import com.ling.lingkb.entity.LingDocument;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Knowledge Source Document Parser Interface
 * <p>
 * Defines the core contract for document parsing, supporting text extraction and metadata collection
 * for multiple document formats.
 * </p>
 *
 * @author shipotian
 * @version 1.0.0
 */
public interface DocumentParser {

    /**
     * Parses the document at the specified path
     *
     * @param filePath the path of the document to be parsed
     * @return parsing result containing text content and metadata
     * @throws Exception when parsing fails
     */
    default LingDocument parse(Path filePath) throws Exception {
        return null;
    }

    /**
     * Parses the document at the specified URL
     *
     * @param url the URL of the document to be parsed
     * @return parsing result containing text content and metadata
     * @throws Exception when parsing fails
     */
    default List<LingDocument> parse(String url) throws Exception {
        return new ArrayList<>();
    }

    /**
     * Gets the set of supported document types
     * <p>
     * Should return file extensions in lowercase format (without "."), e.g.: ["doc", "docx"]
     * </p>
     *
     * @return unmodifiable set of supported types
     */
    Set<String> supportedTypes();

    /**
     * Checks if the specified file type is supported
     *
     * @param fileType file type (case insensitive)
     * @return true if the type is supported
     */
    default boolean supports(String fileType) {
        return supportedTypes().contains(fileType.toLowerCase());
    }

    /**
     * Gets the parser priority (lower value means higher priority)
     * <p>
     * Used to determine the order when multiple parsers support the same type
     * </p>
     *
     * @return priority value, default is 100
     */
    default int getPriority() {
        return 100;
    }
}