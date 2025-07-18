package com.ling.lingkb.controller;

import com.ling.lingkb.entity.Reply;
import com.ling.lingkb.llm.data.DataFeeder;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Knowledge Base Data Backend: Feeding Data
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/30
 */
@Slf4j
@RestController
@RequestMapping("/data")
public class DataController {
    @Value("${system.upload.file.dir}")
    private String uploadFileDir;

    private DataFeeder dataFeeder;

    @Autowired
    public DataController(DataFeeder dataFeeder) {
        this.dataFeeder = dataFeeder;
    }

    @PostMapping("/upload")
    public Reply uploadFile(@RequestParam("file") MultipartFile file) throws Exception {

        if (file.isEmpty()) {
            return Reply.failure("Upload failed, please select a file");
        }

        String fileId = dataFeeder.createFileId();
        String fileName = fileId + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadFileDir).resolve(fileName);
        try {
            file.transferTo(filePath);
        } catch (IOException e) {
            log.error("Upload failed:", e);
            return Reply.failure(e.getMessage());
        }
        dataFeeder.feed(fileId, filePath);
        return Reply.success(fileId);
    }

    @GetMapping("/read")
    public Reply readFile(@RequestParam String fileId) {
        String text = dataFeeder.getFileText(fileId);
        if (StringUtils.isBlank(text)) {
            return Reply.failure("The file does not exist or is being parsed. Please try again later.");
        }
        return Reply.success(text);
    }

}
