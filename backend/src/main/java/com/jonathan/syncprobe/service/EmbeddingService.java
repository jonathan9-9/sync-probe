package com.jonathan.syncprobe.service;

import com.jonathan.syncprobe.model.Chunk;
import com.jonathan.syncprobe.model.EmbeddingChunk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

// langchain4j embedding to generate embeddings
@Service
public class EmbeddingService {
    public List<EmbeddingChunk> embedChunks(List<Chunk> chunks) {
        List<EmbeddingChunk> embeddings = new ArrayList<>();
        if (chunks == null) {
            return embeddings;
        }

        for (Chunk chunk : chunks) {
            // Placeholder vector until real embedding integration is added.
            double[] vector = new double[]{0.0};
            EmbeddingChunk embeddingChunk = new EmbeddingChunk(chunk, vector);
            embeddings.add(embeddingChunk);
        }
        return embeddings;
    }
}
