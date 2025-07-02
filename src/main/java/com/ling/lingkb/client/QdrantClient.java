package com.ling.lingkb.client;
/*
 * ------------------------------------------------------------------
 * Copyright @ 2025 Hangzhou Ling Technology Co.,Ltd. All rights reserved.
 * ------------------------------------------------------------------
 * Product: LingKB
 * Module Name: LingKB
 * Date Created: 2025/7/2
 * Description:
 * ------------------------------------------------------------------
 * Modification History
 * DATE            Name           Description
 * ------------------------------------------------------------------
 * 2025/7/2       spt
 * ------------------------------------------------------------------
 */

import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/7/2
 */
@Slf4j
@Component
@Data
@EqualsAndHashCode(callSuper = false)
@ConfigurationProperties(prefix = "qdrant.client")
public class QdrantClient {
    private QdrantGrpcClient client;
    private String host = "localhost";
    private int port = 6333;
    private String apiKey = "";

    public QdrantClient() {
        client = null;
    }

    @PostConstruct
    public void initClient() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        client = QdrantGrpcClient.newBuilder(channel).withApiKey(apiKey).build();
    }

    public boolean createCollection(String collectionName, int vectorSize) {
        Collections.VectorParams vectorParams =
                Collections.VectorParams.newBuilder().setSize(vectorSize).setDistance(Collections.Distance.Cosine)
                        .build();
        Collections.VectorsConfig vectorsConfig =
                Collections.VectorsConfig.newBuilder().setParams(vectorParams).build();
        Collections.CreateCollection createCollection =
                Collections.CreateCollection.newBuilder().setCollectionName(collectionName)
                        .setVectorsConfig(vectorsConfig).build();
        ListenableFuture<Collections.CollectionOperationResponse> responseListenableFuture =
                client.collections().create(createCollection);
        if (responseListenableFuture.isDone()) {
            try {
                return responseListenableFuture.get().getResult();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public int addVector(String id, float[] textVector, String text, List<String> keywords, String category) {
        Points.Vector vector = Points.Vector.newBuilder().addAllData(floatArrayToList(textVector)).build();
        Points.Vectors vectors = Points.Vectors.newBuilder().setVector(vector).build();
        Points.PointStruct pointStruct =
                Points.PointStruct.newBuilder().setVectors(vectors).setId(Points.PointId.newBuilder().setUuid(id))
                        .putPayload("text", JsonWithInt.Value.newBuilder().setStringValue(text).build())
                        .putPayload("keywords",
                                JsonWithInt.Value.newBuilder().setListValue(stringListToListValue(keywords)).build())
                        .putPayload("category", JsonWithInt.Value.newBuilder().setStringValue(category).build())
                        .build();
        ListenableFuture<Points.PointsOperationResponse> responseListenableFuture =
                client.points().upsert(Points.UpsertPoints.newBuilder().addPoints(pointStruct).build());
        if (responseListenableFuture.isDone()) {
            try {
                return responseListenableFuture.get().getResult().getStatusValue();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    private JsonWithInt.ListValue stringListToListValue(List<String> list) {
        JsonWithInt.ListValue.Builder builder = JsonWithInt.ListValue.newBuilder();
        List<JsonWithInt.Value> values = new ArrayList<>();
        for (String element : list) {
            values.add(JsonWithInt.Value.newBuilder().setStringValue(element).build());
        }
        builder.addAllValues(values);
        return builder.build();
    }

    private List<Float> floatArrayToList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float f : array) {
            list.add(f);
        }
        return list;
    }
}
