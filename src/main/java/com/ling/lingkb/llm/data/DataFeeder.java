package com.ling.lingkb.llm.data;

import com.ling.lingkb.entity.LingDocument;
import com.ling.lingkb.global.SoleMapper;
import com.ling.lingkb.llm.data.extractor.LanguageExtractor;
import com.ling.lingkb.llm.data.parser.DocumentParserFactory;
import com.ling.lingkb.llm.data.processor.TextProcessorFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
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
    private DataFeedDao dataFeedDao;
    @Resource
    private SoleMapper soleMapper;

    @Autowired
    public DataFeeder(DocumentParserFactory parserFactory, TextProcessorFactory processorFactory,
                      LanguageExtractor languageExtractor, DataFeedDao dataFeedDao) {
        this.parserFactory = parserFactory;
        this.processorFactory = processorFactory;
        this.languageExtractor = languageExtractor;
        this.dataFeedDao = dataFeedDao;
    }

    public String createFileId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public void feed(String fileId, Path filePath) throws Exception {
        LingDocument lingDocument = parserFactory.parse(filePath.toFile());
        lingDocument = processorFactory.process(lingDocument);
        lingDocument = languageExtractor.doExtract(lingDocument);
        lingDocument.setFileId(fileId);
        lingDocument.setWorkspace(workspace);
        lingDocument.setSize(Files.size(filePath));
        Files.deleteIfExists(filePath);
        soleMapper.saveDocument(lingDocument);
        dataFeedDao.feed(lingDocument);
    }

    public List<LingDocument> getFileIdList() {
        return soleMapper.queryMajorByWorkspace(workspace);
    }

    public LingDocument getDocument(String fileId) {
        return soleMapper.queryDocumentByFileId(fileId);
    }
}
