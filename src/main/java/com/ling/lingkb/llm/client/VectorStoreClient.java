package com.ling.lingkb.llm.client;

import com.ling.lingkb.entity.LingVector;
import com.ling.lingkb.global.SoleMapper;
import io.github.jbellis.jvector.disk.ReaderSupplier;
import io.github.jbellis.jvector.disk.ReaderSupplierFactory;
import io.github.jbellis.jvector.graph.GraphIndexBuilder;
import io.github.jbellis.jvector.graph.GraphSearcher;
import io.github.jbellis.jvector.graph.OnHeapGraphIndex;
import io.github.jbellis.jvector.graph.RandomAccessVectorValues;
import io.github.jbellis.jvector.graph.SearchResult;
import io.github.jbellis.jvector.graph.disk.OnDiskGraphIndex;
import io.github.jbellis.jvector.graph.similarity.BuildScoreProvider;
import io.github.jbellis.jvector.graph.similarity.DefaultSearchScoreProvider;
import io.github.jbellis.jvector.graph.similarity.SearchScoreProvider;
import io.github.jbellis.jvector.util.Bits;
import static io.github.jbellis.jvector.vector.VectorSimilarityFunction.COSINE;
import io.github.jbellis.jvector.vector.VectorizationProvider;
import io.github.jbellis.jvector.vector.types.VectorFloat;
import io.github.jbellis.jvector.vector.types.VectorTypeSupport;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/7/15
 */
@Slf4j
@Component
public class VectorStoreClient {
    @Value("${system.workspace}")
    private String workspace;
    @Value("${vector.data.path}")
    private String vectorDataPath;
    @Value("${vector.bak.path}")
    private String vectorBakPath;
    @Value("${vector.default.dimension}")
    private int vectorDefaultDimension;
    @Value("${vector.search.top}")
    private int vectorSearchTop;
    @Value("${vector.search.score}")
    private float vectorSearchScore;

    private static final VectorTypeSupport VTS = VectorizationProvider.getInstance().getVectorTypeSupport();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private OnDiskGraphIndex diskIndex;
    private MutableVectorValues vectorValues;

    @Resource
    SoleMapper soleMapper;

    @PostConstruct
    public void init() throws IOException {
        loadVectors();
        loadIndex();
    }

    private void loadVectors() {
        lock.writeLock().lock();
        try {
            vectorValues = new MutableVectorValues(vectorDefaultDimension);
            List<LingVector> lingVectors = soleMapper.queryPersistedVectors(workspace);
            vectorValues.addAll(lingVectors);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void loadIndex() throws IOException {
        lock.writeLock().lock();
        try {
            int vectorSize = vectorValues.size();
            boolean indexExist = Files.exists(Path.of(vectorDataPath));
            boolean vectorExist = vectorSize > 0;
            if (vectorExist) {
                if (!indexExist) {
                    createIndex();
                }
                ReaderSupplier rs = ReaderSupplierFactory.open(Path.of(vectorDataPath));
                diskIndex = OnDiskGraphIndex.load(rs);
            } else if (indexExist) {
                log.warn("There are index file but no vectors. The file will be move to ensure data consistency.");
                Files.deleteIfExists(Path.of(vectorBakPath));
                Files.move(Path.of(vectorDataPath), Path.of(vectorBakPath), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void createIndex() {
        lock.writeLock().lock();
        BuildScoreProvider bsp = BuildScoreProvider.randomAccessScoreProvider(vectorValues, COSINE);
        try (GraphIndexBuilder builder = new GraphIndexBuilder(bsp, vectorDefaultDimension, 16, 100, 1.2f, 1.2f, false,
                true)) {
            Path parentDir = Path.of(vectorDataPath).getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            OnHeapGraphIndex heapIndex = builder.build(vectorValues);
            OnDiskGraphIndex.write(heapIndex, vectorValues, Path.of(vectorDataPath));
        } catch (IOException e) {
            log.error("Failed to create index", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Scheduled(fixedRate = 60000)
    public void persistedSave() {
        lock.writeLock().lock();
        try {
            log.info("Begin persisting the vector index data");
            List<LingVector> lingVectors = soleMapper.queryUnPersistedVectors(workspace);
            if (!lingVectors.isEmpty()) {
                if (diskIndex != null) {
                    Files.deleteIfExists(Path.of(vectorBakPath));
                    Files.move(Path.of(vectorDataPath), Path.of(vectorBakPath), StandardCopyOption.REPLACE_EXISTING);
                }
                int lastMaxNodeId = Optional.ofNullable(soleMapper.queryMaxNodeId(workspace)).orElse(-1);
                vectorValues.addAll(lingVectors);
                BuildScoreProvider bsp = BuildScoreProvider.randomAccessScoreProvider(vectorValues, COSINE);
                try (GraphIndexBuilder builder = new GraphIndexBuilder(bsp, vectorDefaultDimension, 16, 100, 1.2f, 1.2f,
                        false, true)) {
                    OnHeapGraphIndex heapIndex = builder.build(vectorValues);
                    OnDiskGraphIndex.write(heapIndex, vectorValues, Path.of(vectorDataPath));
                    loadIndex();
                }
                soleMapper.updateUnPersistedVectors(workspace, lastMaxNodeId);
                log.info("Persisting completed");
            }
        } catch (IOException e) {
            log.error("Failed to persist index", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void addVectors(String docId, List<String> texts, List<float[]> vectors) {
        lock.writeLock().lock();
        try {
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
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String searchTopOne(float[] query) {
        List<String> result = searchTopK(query, 1);
        if (result.size() >= 1) {
            return result.get(0);
        }
        return "";
    }

    public List<String> searchTopK(float[] query) {
        return searchTopK(query, vectorSearchTop);
    }

    private List<String> searchTopK(float[] query, int k) {
        lock.readLock().lock();
        try {
            VectorFloat<?> queryVector = VTS.createFloatVector(query);
            SearchResult sr = GraphSearcher.search(queryVector, k, vectorValues, COSINE, diskIndex, Bits.ALL);
            return queryVectorTxt(sr);
        } finally {
            lock.readLock().unlock();
        }
    }

    private List<String> searchTopKExactMatches(float[] query, int k) throws IOException {
        lock.readLock().lock();
        try (GraphSearcher searcher = new GraphSearcher(diskIndex)) {
            VectorFloat<?> queryVector = VTS.createFloatVector(query);
            SearchScoreProvider ssp = DefaultSearchScoreProvider.exact(queryVector, COSINE, vectorValues);
            SearchResult sr = searcher.search(ssp, k, Bits.ALL);
            return queryVectorTxt(sr);
        } finally {
            lock.readLock().unlock();
        }
    }

    private List<String> queryVectorTxt(SearchResult sr) {
        List<Integer> nodeIds = new ArrayList<>();
        for (SearchResult.NodeScore nodeScore : sr.getNodes()) {
            if (nodeScore.score >= vectorSearchScore) {
                nodeIds.add(nodeScore.node);
            }
        }
        if (nodeIds.isEmpty()) {
            return new ArrayList<>();
        } else {
            return soleMapper.queryVectorTxtByNodeIds(workspace, nodeIds);
        }
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

    private float[] stringToFloats(String str) {
        float[] array = new float[vectorDefaultDimension];
        int currentPos = 0;
        int nextComma;
        int index = 0;
        final int length = str.length();

        while (currentPos < length && index < vectorDefaultDimension) {
            nextComma = str.indexOf(',', currentPos);
            if (nextComma == -1) {
                nextComma = length;
            }

            array[index++] = Float.parseFloat(str.substring(currentPos, nextComma).trim());
            currentPos = nextComma + 1;
        }
        return array;
    }

    public class MutableVectorValues implements RandomAccessVectorValues {
        private final List<VectorFloat<?>> vectors = new ArrayList<>();
        private final int dimension;

        MutableVectorValues(int dimension) {
            this.dimension = dimension;
        }

        void addAll(List<LingVector> lingVectors) {
            for (LingVector lingVector : lingVectors) {
                float[] vector = stringToFloats(lingVector.getVector());
                if (vector.length != dimension) {
                    throw new IllegalArgumentException(
                            String.format("Vector dimension mismatch. Expected %d, got %d", dimension, vector.length));
                }
                vectors.add(VTS.createFloatVector(vector));
            }
        }

        @Override
        public int size() {
            return vectors.size();
        }

        @Override
        public int dimension() {
            return dimension;
        }

        @Override
        public VectorFloat<?> getVector(int i) {
            return vectors.get(i);
        }

        @Override
        public boolean isValueShared() {
            return false;
        }

        @Override
        public RandomAccessVectorValues copy() {
            MutableVectorValues copy = new MutableVectorValues(dimension);
            copy.vectors.addAll(this.vectors);
            return copy;
        }
    }

}