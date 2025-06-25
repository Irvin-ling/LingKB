package com.ling.lingkb.util;


import com.ling.lingkb.util.language.LanguageUtil;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/24
 */
public class SimHashUtil {
    private static final int HASH_BITS = 64;

    private static long simHash(String text) {
        if (text == null || text.isEmpty()) {
            return 0L;
        }
        List<String> words = LanguageUtil.tokenize(text);
        long[] features = new long[HASH_BITS];
        for (String word : words) {
            long hash = hash(word);
            double weight = 1.0;
            for (int i = 0; i < HASH_BITS; i++) {
                long bitMask = 1L << i;
                if ((hash & bitMask) != 0) {
                    features[i] += weight;
                } else {
                    features[i] -= weight;
                }
            }
        }
        long simHash = 0;
        for (int i = 0; i < HASH_BITS; i++) {
            if (features[i] > 0) {
                simHash |= (1L << i);
            }
        }

        return simHash;
    }

    /**
     * Calculate the Hamming distance between two SimHash values.
     */
    private static int hammingDistance(long hash1, long hash2) {
        long xor = hash1 ^ hash2;
        int distance = 0;

        while (xor != 0) {
            distance += xor & 1;
            xor >>>= 1;
        }

        return distance;
    }

    /**
     * Calculate the similarity (0.0-1.0) between two SimHash values.
     */
    private static double similarity(long hash1, long hash2) {
        int distance = hammingDistance(hash1, hash2);
        return 1.0 - (double) distance / HASH_BITS;
    }

    /**
     * hash
     */
    private static long hash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));

            long hash = 0;
            for (int i = 0; i < 8; i++) {
                hash <<= 8;
                hash |= (digest[i] & 0xFF);
            }
            return hash;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Use SimHash for text deduplication.
     */
    public static List<String> deduplicate(List<String> texts, double threshold) {
        List<String> uniqueTexts = new ArrayList<>();
        Map<Long, String> simHashMap = new HashMap<>();

        for (String text : texts) {
            long simHash = simHash(text);
            boolean isDuplicate = false;

            for (Map.Entry<Long, String> entry : simHashMap.entrySet()) {
                if (similarity(simHash, entry.getKey()) >= threshold) {
                    isDuplicate = true;
                    break;
                }
            }

            if (!isDuplicate) {
                uniqueTexts.add(text);
                simHashMap.put(simHash, text);
            }
        }

        return uniqueTexts;
    }
}