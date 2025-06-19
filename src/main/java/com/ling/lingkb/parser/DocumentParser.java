package com.ling.lingkb.parser;
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

import com.ling.lingkb.common.entity.DocumentParseResult;
import com.ling.lingkb.common.exception.DocumentParseException;
import java.io.File;
import java.util.List;

/**
 * 知识源文档解析器
 *
 * @author shipotian
 * @date 2025/6/19
 * @since 1.0.0
 */
public interface DocumentParser {
    /**
     * 解析文档并返回文本内容和元数据
     */
    DocumentParseResult parse(File file) throws DocumentParseException;

    /**
     * 支持的文档类型
     */
    List<String> supportedTypes();
}
