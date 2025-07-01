package com.ling.lingkb.util;

import com.hankcs.hanlp.HanLP;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/27
 */
public class ResourceUtil {

    public static String getResource(String path) throws IOException {
        Resource resource = new ClassPathResource(path);
        return Files.readString(resource.getFile().toPath(), StandardCharsets.UTF_8);
    }

    public static String getModelPath(String fileName) {
        String dependencyModelPath = HanLP.Config.PerceptronParserModelPath;
        File modelFile = new File(dependencyModelPath);
        File modelDir = modelFile.getParentFile().getParentFile();
        return modelDir.getAbsolutePath().concat(File.separator).concat(fileName);
    }
}
