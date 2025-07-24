package com.ling.lingkb.global;

import com.hankcs.hanlp.utility.SentencesUtil;
import com.ling.lingkb.entity.LingDocument;
import com.ling.lingkb.entity.LingDocumentLink;
import com.ling.lingkb.entity.LingVector;
import com.ling.lingkb.llm.client.EmbeddingClient;
import com.ling.lingkb.llm.client.VectorStoreClient;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Work dao
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2025/7/17
 */
@Component
public class AsyncDao {

    @Bean(name = "restTemplate")
    public RestTemplate httpsRestTemplate() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
        SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
        SSLConnectionSocketFactory connectionSocketFactory =
                new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
        HttpClientBuilder httpClientBuilder = HttpClients.custom().disableAutomaticRetries();
        httpClientBuilder.setSSLSocketFactory(connectionSocketFactory);
        HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;
        SSLConnectionSocketFactory sslConnectionSocketFactory =
                new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslConnectionSocketFactory).build();
        PoolingHttpClientConnectionManager poolingHttpClientConnectionManager =
                new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        poolingHttpClientConnectionManager.setMaxTotal(5400);
        poolingHttpClientConnectionManager.setDefaultMaxPerRoute(300);
        httpClientBuilder.setConnectionManager(poolingHttpClientConnectionManager);
        CloseableHttpClient httpClient = httpClientBuilder.build();
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(60 * 1000);
        factory.setReadTimeout(60 * 1000);
        factory.setConnectionRequestTimeout(60 * 1000);
        factory.setHttpClient(httpClient);
        RestTemplate restTemplate = new RestTemplate(factory);
        List<HttpMessageConverter<?>> list = restTemplate.getMessageConverters();
        for (HttpMessageConverter converter : list) {
            if (converter instanceof StringHttpMessageConverter) {
                ((StringHttpMessageConverter) converter).setDefaultCharset(Charset.forName("UTF-8"));
                break;
            }
        }
        return restTemplate;
    }

    @Value("${qwen.embedding.chunk.size}")
    private int qwenEmbeddingChunkSize;
    @Value("${system.workspace}")
    private String workspace;
    @Resource
    private SoleMapper soleMapper;
    private EmbeddingClient embeddingClient;
    private VectorStoreClient vectorStoreClient;

    @Autowired
    public AsyncDao(EmbeddingClient embeddingClient, VectorStoreClient vectorStoreClient) {
        this.embeddingClient = embeddingClient;
        this.vectorStoreClient = vectorStoreClient;
    }

    @Async
    public void feed(LingDocument lingDocument) {
        List<String> sentences = SentencesUtil.toSentenceList(lingDocument.getText(), false);
        List<List<String>> sentenceChunks = splitIntoChunks(sentences);
        for (List<String> sentenceChunk : sentenceChunks) {
            feedInChunk(lingDocument.getDocId(), sentenceChunk);
        }
        feedLinks(lingDocument.getLinks());
        vectorStoreClient.setToInconsistent();
    }

    @Async
    public void feedInChunk(String docId, List<String> texts) {
        List<float[]> vectors = embeddingClient.getEmbeddings(texts);
        List<LingVector> vectorList = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            float[] vector = vectors.get(i);
            LingVector lingVector =
                    LingVector.builder().workspace(workspace).docId(docId).txt(text).vector(floatsToString(vector))
                            .persisted(false).build();
            vectorList.add(lingVector);
        }
        soleMapper.batchSaveVectors(vectorList);
    }

    private void feedLinks(List<LingDocumentLink> links) {
        if (links != null && !links.isEmpty()) {
            List<String> descList = links.stream().map(LingDocumentLink::getDescText).collect(Collectors.toList());
            List<float[]> descVectorList = embeddingClient.getEmbeddings(descList);
            for (int i = 0; i < links.size(); i++) {
                LingDocumentLink link = links.get(i);
                float[] vector = descVectorList.get(i);
                link.setDescVector(floatsToString(vector));
            }
            soleMapper.batchSaveLinks(links);
        }
    }

    @Async
    public void removeNode(int nodeId) {
        soleMapper.removeVectorByNodeId(nodeId);
        vectorStoreClient.setToInconsistent();
    }

    private List<List<String>> splitIntoChunks(List<String> sentences) {
        List<List<String>> chunks = new ArrayList<>();
        if (sentences == null || sentences.isEmpty()) {
            return chunks;
        }

        int totalSize = sentences.size();
        for (int i = 0; i < totalSize; i += qwenEmbeddingChunkSize) {
            int end = Math.min(i + qwenEmbeddingChunkSize, totalSize);
            List<String> chunk = sentences.subList(i, end);
            chunks.add(chunk);
        }
        return chunks;
    }

    private static String floatsToString(float[] array) {
        StringBuilder sb = new StringBuilder();
        sb.append(array[0]);
        final String delimiter = ",";
        for (int i = 1; i < array.length; i++) {
            sb.append(delimiter).append(array[i]);
        }
        return sb.toString();
    }
}
