package com.ling.lingkb.llm.data;

import com.ling.lingkb.entity.LingDocument;
import com.ling.lingkb.entity.LingVector;
import com.ling.lingkb.global.AsyncDao;
import com.ling.lingkb.global.SoleMapper;
import com.ling.lingkb.llm.data.extractor.LanguageExtractor;
import com.ling.lingkb.llm.data.parser.DocumentParserFactory;
import com.ling.lingkb.llm.data.processor.TextProcessorFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Working area when uploading files to LingKB
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2025/7/17
 */
@Component
public class DataFeeder {
    @Value("${system.workspace}")
    private String workspace;

    private DocumentParserFactory parserFactory;
    private TextProcessorFactory processorFactory;
    private LanguageExtractor languageExtractor;
    private AsyncDao asyncDao;
    @Resource
    private SoleMapper soleMapper;

    @Autowired
    public DataFeeder(DocumentParserFactory parserFactory, TextProcessorFactory processorFactory,
                      LanguageExtractor languageExtractor, AsyncDao asyncDao) {
        this.parserFactory = parserFactory;
        this.processorFactory = processorFactory;
        this.languageExtractor = languageExtractor;
        this.asyncDao = asyncDao;
    }

    public String createDocId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public void feed(String docId, Path filePath) throws Exception {
        LingDocument lingDocument = parserFactory.parse(filePath.toFile());
        processAndExtract(lingDocument, docId);
        Files.deleteIfExists(filePath);
    }

    public String feed(String url, String type) throws Exception {
        List<LingDocument> lingDocuments = parserFactory.parseUrl(url, type);
        lingDocuments.forEach(lingDocument -> processAndExtract(lingDocument, createDocId()));
        return lingDocuments.stream().map(LingDocument::getDocId).collect(Collectors.joining(","));
    }

    private void processAndExtract(LingDocument lingDocument, String docId) {
        lingDocument = processorFactory.process(lingDocument);
        lingDocument = languageExtractor.doExtract(lingDocument);
        lingDocument.setDocId(docId);
        lingDocument.setWorkspace(workspace);
        soleMapper.saveDocument(lingDocument);
        asyncDao.feed(lingDocument);
    }

    public List<LingDocument> getDocIdList() {
        return soleMapper.queryDocument(workspace);
    }

    public LingDocument getDocument(String docId) {
        LingDocument lingDocument = soleMapper.queryDocumentByDocId(docId);
        LingVector lingVector = soleMapper.queryVectorByDocId(docId);
        lingDocument.setPersisted(lingVector.isPersisted());
        return lingDocument;
    }

    public List<LingVector> getVectors(String docId) {
        return soleMapper.queryVectorsByDocId(docId);
    }

    public void removeNode(int nodeId) {
        asyncDao.removeNode(nodeId);
    }

    public void updateNode(String docId, int nodeId, String txt) {
        asyncDao.removeNode(nodeId);
        asyncDao.feedInChunk(docId, Collections.singletonList(txt));
    }

}
