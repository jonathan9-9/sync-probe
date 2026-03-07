package com.jonathan.syncprobe.service;

import com.jonathan.syncprobe.model.Chunk;
import com.jonathan.syncprobe.model.EmbeddingChunk;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

// langchain4j embedding to generate embeddings
@Service
public class EmbeddingService {
    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public List<EmbeddingChunk> embedChunks(List<Chunk> chunks) {
        List<EmbeddingChunk> embeddings = new ArrayList<>();
        if (chunks == null) {
            return embeddings;
        }

        for (Chunk chunk : chunks) {
            double[] vector = embedContent(chunk == null ? null : chunk.getContent());
            EmbeddingChunk embeddingChunk = new EmbeddingChunk(chunk, vector);
            embeddings.add(embeddingChunk);
        }
        return embeddings;
    }

    public double[] embedText(String text) {
        return embedContent(text);
    }

    private double[] embedContent(String content) {
        if (content == null || content.isBlank()) {
            return new double[]{0.0};
        }
        try {
            float[] vector = embeddingModel.embed(TextSegment.from(content)).content().vector();
            if (vector == null || vector.length == 0) {
                throw new IllegalStateException("Embedding model returned an empty vector");
            }
            double[] dense = new double[vector.length];
            for (int i = 0; i < vector.length; i++) {
                dense[i] = vector[i];
            }
            return dense;
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to generate embedding for provided text", e);
        }
    }
}
