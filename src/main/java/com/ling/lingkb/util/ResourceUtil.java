package com.ling.lingkb.util;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.io.IOUtil;
import java.io.File;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/27
 */
public class ResourceUtil {

    public static String getResource(String path) {
        if (IOUtil.isFileExisted(path)) {
            return IOUtil.readTxt(path);
        } else {
            return "";
        }
    }

    public static String getModelPath(String fileName) {
        String dependencyModelPath = HanLP.Config.PerceptronParserModelPath;
        File modelFile = new File(dependencyModelPath);
        File modelDir = modelFile.getParentFile().getParentFile();
        return modelDir.getAbsolutePath().concat(File.separator).concat(fileName);
    }
}
