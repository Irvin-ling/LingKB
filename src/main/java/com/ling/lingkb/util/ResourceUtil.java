package com.ling.lingkb.util;
/*
 * ------------------------------------------------------------------
 * Copyright @ 2025 Hangzhou Ling Technology Co.,Ltd. All rights reserved.
 * ------------------------------------------------------------------
 * Product: LingKB
 * Module Name: LingKB
 * Date Created: 2025/6/27
 * Description:
 * ------------------------------------------------------------------
 * Modification History
 * DATE            Name           Description
 * ------------------------------------------------------------------
 * 2025/6/27       spt
 * ------------------------------------------------------------------
 */

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/27
 */
public class ResourceUtil {

    public static String getResource(String path) {
        try {
            InputStream inputStream = new ClassPathResource(path).getInputStream();
            return IOUtils.toString(inputStream, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
