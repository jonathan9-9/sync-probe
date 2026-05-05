package com.jonathan.syncprobe.service;

import com.jonathan.syncprobe.model.Chunk;
import com.jonathan.syncprobe.model.EmbeddingChunk;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

// langchain4j embedding to generate embeddings
@Service
public class EmbeddingService {
    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
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
            String content = chunk == null ? null : chunk.getContent();
            double[] vector = embedContent(content, chunk);
            EmbeddingChunk embeddingChunk = new EmbeddingChunk(chunk, vector);
            embeddings.add(embeddingChunk);
        }
        return embeddings;
    }

    public double[] embedText(String text) {
        return embedContent(text, null);
    }

    private double[] embedContent(String content, Chunk chunk) {
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
            String context = chunkContext(chunk, content);
            log.error("Embedding generation failed. {}", context, e);
            throw new RuntimeException("Failed to generate embedding for provided text. " + context, e);
        }
    }

    private String chunkContext(Chunk chunk, String content) {
        int length = content == null ? 0 : content.length();
        if (chunk == null) {
            return "context={type=query,path=unknown,id=unknown,length=" + length + "}";
        }

        String id = chunk.getId() == null ? "unknown" : chunk.getId();
        String path = chunk.getPath() == null ? "unknown" : chunk.getPath();
        String type = chunk.getType() == null ? "unknown" : chunk.getType();
        String symbol = chunk.getSymbol() == null ? "unknown" : chunk.getSymbol();
        return "context={type=" + type + ",path=" + path + ",id=" + id + ",symbol=" + symbol + ",length=" + length + "}";
    }
}
