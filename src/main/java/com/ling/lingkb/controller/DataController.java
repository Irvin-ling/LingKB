package com.ling.lingkb.controller;

import com.ling.lingkb.entity.LingDocument;
import com.ling.lingkb.entity.Reply;
import com.ling.lingkb.llm.data.DataFeeder;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
@CrossOrigin(origins = {"http://127.0.0.1:8080", "http://localhost:8080"}, allowCredentials = "true") // TODO to remove
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

        String docId = dataFeeder.createDocId();
        String fileName = docId + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadFileDir).resolve(fileName);
        try {
            file.transferTo(filePath);
        } catch (IOException e) {
            log.error("Upload failed:", e);
            return Reply.failure(e.getMessage());
        }
        dataFeeder.feed(docId, filePath);
        return Reply.success(docId);
    }

    @GetMapping("/parse")
    public Reply parseUrl(@RequestParam("url") String url, @RequestParam("type") String type) throws Exception {
        url = URLDecoder.decode(url, StandardCharsets.UTF_8);
        if (isValidUrl(url)) {
            String docId = dataFeeder.createDocId();
            dataFeeder.feed(docId, url, type);
            return Reply.success(docId);
        } else {
            return Reply.failure("Incorrect URL format");
        }
    }

    @GetMapping("/docs")
    public Reply docs() {
        List<LingDocument> docs = dataFeeder.getDocIdList();
        return Reply.success(docs);
    }

    @GetMapping("/docs/{docId}")
    public Reply doc(@PathVariable String docId) {
        LingDocument document = dataFeeder.getDocument(docId);
        if (document == null) {
            return Reply.failure("The doc does not exist or is being parsed. Please try again later.");
        }
        return Reply.success(document);
    }

    @GetMapping("/vectors/{docId}")
    public Reply vectors(@PathVariable String docId) {
        return Reply.success(dataFeeder.getVectors(docId));
    }

    @DeleteMapping("/vectors/{nodeId}")
    public Reply removeNode(@PathVariable int nodeId) {
        dataFeeder.removeNode(nodeId);
        return Reply.success();
    }

    @PutMapping("/vectors/{docId}/{nodeId}")
    public Reply updateNode(@PathVariable String docId, @PathVariable int nodeId, @RequestBody String newTxt) {
        dataFeeder.updateNode(docId, nodeId, newTxt);
        return Reply.success();
    }

    private static boolean isValidUrl(String url) {
        try {
            URI uri = new URI(url);
            return uri.getScheme() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
