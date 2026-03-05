package com.jonathan.syncprobe.service;

import com.jonathan.syncprobe.model.Chunk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

// langchain4j embedding to generate embeddings
@Service
public class EmbeddingService {
    public List<double[]> embedChunks(List<Chunk> chunks) {
        List<double[]> embeddings = new ArrayList<>();
        if (chunks == null) {
            return embeddings;
        }

        for (Chunk ignored : chunks) {
            // Placeholder vector until real embedding integration is added.
            embeddings.add(new double[]{0.0});
        }
        return embeddings;
    }
}
