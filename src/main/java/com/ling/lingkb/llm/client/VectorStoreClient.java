package com.ling.lingkb.llm.client;

import com.ling.lingkb.entity.LingDocumentLink;
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
import java.util.concurrent.atomic.AtomicBoolean;
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
    @Value("${vector.link.path}")
    private String vectorLinkPath;
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
    private OnDiskGraphIndex linkDiskIndex;
    private MutableVectorValues linkVectorValues;
    private AtomicBoolean consistent = new AtomicBoolean(true);

    @Resource
    SoleMapper soleMapper;

    @PostConstruct
    public void init() {
        lock.writeLock().lock();
        try {
            soleMapper.resetVector(workspace);
            vectorValues = new MutableVectorValues(vectorDefaultDimension);
            List<LingVector> lingVectors = soleMapper.queryAllVector(workspace);
            vectorValues.addAll(lingVectors);
            if (vectorValues.size() > 0) {
                BuildScoreProvider bsp = BuildScoreProvider.randomAccessScoreProvider(vectorValues, COSINE);
                try (GraphIndexBuilder builder = new GraphIndexBuilder(bsp, vectorDefaultDimension, 16, 100, 1.2f, 1.2f,
                        false, true)) {
                    Path dataPath = Path.of(vectorDataPath);
                    Path bakPath = Path.of(vectorBakPath);
                    Path parentDir = dataPath.getParent();
                    if (parentDir != null) {
                        Files.createDirectories(parentDir);
                    }
                    Files.deleteIfExists(bakPath);
                    if (Files.exists(dataPath)) {
                        Files.move(dataPath, bakPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    OnHeapGraphIndex heapIndex = builder.build(vectorValues);
                    OnDiskGraphIndex.write(heapIndex, vectorValues, dataPath);
                    ReaderSupplier rs = ReaderSupplierFactory.open(dataPath);
                    diskIndex = OnDiskGraphIndex.load(rs);
                } catch (IOException e) {
                    log.error("Failed to create index", e);
                }
            } else {
                diskIndex = null;
            }

            linkVectorValues = new MutableVectorValues(vectorDefaultDimension);
            List<LingDocumentLink> links = soleMapper.queryLinkVectors(workspace);
            linkVectorValues.addLinks(links);
            if (linkVectorValues.size() > 0) {
                BuildScoreProvider bsp = BuildScoreProvider.randomAccessScoreProvider(linkVectorValues, COSINE);
                try (GraphIndexBuilder builder = new GraphIndexBuilder(bsp, vectorDefaultDimension, 16, 100, 1.2f, 1.2f,
                        false, true)) {
                    Path linkPath = Path.of(vectorLinkPath);
                    OnHeapGraphIndex heapIndex = builder.build(linkVectorValues);
                    OnDiskGraphIndex.write(heapIndex, linkVectorValues, linkPath);
                    ReaderSupplier rs = ReaderSupplierFactory.open(linkPath);
                    linkDiskIndex = OnDiskGraphIndex.load(rs);
                } catch (IOException e) {
                    log.error("Failed to create index", e);
                }
            } else {
                linkDiskIndex = null;
            }
            consistent.set(true);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Scheduled(fixedRate = 600_000)
    public void persistedSave() {
        lock.writeLock().lock();
        try {
            log.info("Begin persisting the vector index data");
            if (!consistent.get()) {
                init();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void setToInconsistent() {
        consistent.set(false);
    }

    public List<String> searchTopK(float[] query) {
        lock.readLock().lock();
        try {
            if (diskIndex == null) {
                return new ArrayList<>();
            }
            VectorFloat<?> queryVector = VTS.createFloatVector(query);
            SearchResult sr =
                    GraphSearcher.search(queryVector, vectorSearchTop, vectorValues, COSINE, diskIndex, Bits.ALL);
            return queryVectorTxt(sr);
        } finally {
            lock.readLock().unlock();
        }
    }

    private List<String> queryVectorTxt(SearchResult sr) {
        List<Integer> nodeIds = getNodeIds(sr);
        if (nodeIds.isEmpty()) {
            return new ArrayList<>();
        } else {
            return soleMapper.queryVectorTxtByNodeIds(workspace, nodeIds);
        }
    }

    private LingDocumentLink queryLink(SearchResult sr) {
        List<Integer> nodeIds = getNodeIds(sr);
        if (nodeIds.isEmpty()) {
            return null;
        } else {
            return soleMapper.queryLink(workspace, nodeIds.get(0));
        }
    }

    private List<Integer> getNodeIds(SearchResult sr) {
        List<Integer> nodeIds = new ArrayList<>();
        for (SearchResult.NodeScore nodeScore : sr.getNodes()) {
            if (nodeScore.score >= vectorSearchScore) {
                nodeIds.add(nodeScore.node);
            }
        }
        return nodeIds;
    }

    public LingDocumentLink searchLink(float[] query) {
        lock.readLock().lock();
        try {
            if (linkDiskIndex == null) {
                return null;
            }
            VectorFloat<?> queryVector = VTS.createFloatVector(query);
            SearchResult sr =
                    GraphSearcher.search(queryVector, 1, linkVectorValues, COSINE, linkDiskIndex, Bits.ALL);
            return queryLink(sr);
        } finally {
            lock.readLock().unlock();
        }
    }

    private List<String> searchTopExactMatches(float[] query, int k) throws IOException {
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

        void addLinks(List<LingDocumentLink> links) {
            for (LingDocumentLink link : links) {
                float[] vector = stringToFloats(link.getDescVector());
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