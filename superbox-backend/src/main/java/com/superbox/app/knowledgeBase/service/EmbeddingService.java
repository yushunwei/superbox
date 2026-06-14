package com.superbox.app.knowledgeBase.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class EmbeddingService {

    private static final int EMBEDDING_DIM = 1536;
    private final Random rng = new Random(42);

    public List<Float> embed(String text) {
        int seed = text.hashCode();
        Random localRng = new Random(seed);
        List<Float> vector = new ArrayList<>(EMBEDDING_DIM);
        for (int i = 0; i < EMBEDDING_DIM; i++) {
            vector.add((float) localRng.nextGaussian());
        }
        double norm = 0;
        for (float v : vector) norm += v * v;
        norm = Math.sqrt(norm);
        double n = norm;
        vector.replaceAll(v -> (float)(v / n));
        log.debug("Generated embedding for text (len={}, dim={})", text.length(), EMBEDDING_DIM);
        return vector;
    }

    public String toPgVectorString(List<Float> embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding.get(i));
        }
        sb.append("]");
        return sb.toString();
    }
}
