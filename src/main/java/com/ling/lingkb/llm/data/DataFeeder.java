package com.ling.lingkb.llm.data;

import com.ling.lingkb.entity.LingDocument;
import com.ling.lingkb.global.SoleMapper;
import com.ling.lingkb.llm.data.extractor.LanguageExtractor;
import com.ling.lingkb.llm.data.parser.DocumentParserFactory;
import com.ling.lingkb.llm.data.processor.TextProcessorFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
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

    @Async
    public void feed(String fileId, Path filePath) throws Exception {
        LingDocument lingDocument = parserFactory.parse(filePath.toFile());
        lingDocument = processorFactory.process(lingDocument);
        lingDocument = languageExtractor.doExtract(lingDocument);
        lingDocument.setFileId(fileId);
        lingDocument.setWorkspace(workspace);
        Files.deleteIfExists(filePath);
        soleMapper.saveDocument(lingDocument);
        dataFeedDao.feed(lingDocument);
    }

    public String getFileText(String fileId) {
        LingDocument lingDocument = soleMapper.queryDocumentByFileId(fileId);
        if (lingDocument == null) {
            return null;
        }
        return lingDocument.getText();
    }
}
