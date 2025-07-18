package com.ling.lingkb.global;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/7/18
 */
@Slf4j
public class LingInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment env = applicationContext.getEnvironment();
        String uploadFileDir = env.getProperty("system.upload.file.dir");
        if (StringUtils.isBlank(uploadFileDir)) {
            throw new RuntimeException();
        }
        createDir(Paths.get(uploadFileDir));

        String uploadFile = env.getProperty("vector.data.path");
        if (StringUtils.isBlank(uploadFile)) {
            throw new RuntimeException();
        }
        createDir(Path.of(uploadFile).getParent());

        String embeddingUrl = env.getProperty("qwen.embedding.url");
        if (StringUtils.isBlank(embeddingUrl)) {
            throw new RuntimeException();
        }
        String chatUrl = env.getProperty("qwen.chat.url");
        if (StringUtils.isBlank(chatUrl)) {
            throw new RuntimeException();
        }

        String imageSwitch = env.getProperty("data.parser.image.switch");
        if (Boolean.parseBoolean(imageSwitch)) {
            String dataTessPath = env.getProperty("data.parser.tess.path");
            createDir(Path.of(dataTessPath));
        }
    }

    private void createDir(Path dirPath) {
        if (dirPath != null && Files.notExists(dirPath)) {
            try {
                Files.createDirectories(dirPath);
                log.warn("{} path does not exist, the folder will be created automatically.", dirPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
