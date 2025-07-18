package com.ling.lingkb;

import com.ling.lingkb.global.LingInitializer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


/**
 * @author shipotian
 * @date 2025/6/19
 * @since 1.0.0
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class LingkbApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(LingkbApplication.class).initializers(new LingInitializer()).run(args);
    }

}
