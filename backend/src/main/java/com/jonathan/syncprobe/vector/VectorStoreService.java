package com.jonathan.syncprobe.vector;

import com.jonathan.syncprobe.model.Chunk;
import com.jonathan.syncprobe.model.EmbeddingChunk;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VectorStoreService {
    private static final int DEFAULT_SEARCH_RESULTS = 8;
    private static final double LOW_SIMILARITY_THRESHOLD = 0.55;

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final Map<String, Chunk> chunkByEmbeddingId = new HashMap<>();
    private final Map<String, float[]> vectorByEmbeddingId = new HashMap<>();
    private final List<String> storedDocEmbeddingIds = new ArrayList<>();

    public VectorStoreService(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel){
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    public void store(List<EmbeddingChunk> embeddings) {
        chunkByEmbeddingId.clear();
        vectorByEmbeddingId.clear();
        storedDocEmbeddingIds.clear();

        if (embeddings == null || embeddings.isEmpty()) {
            return;
        }

        for (EmbeddingChunk embeddingChunk : embeddings) {
            if (embeddingChunk == null || embeddingChunk.getChunk() == null || embeddingChunk.getEmbedding() == null) {
                continue;
            }

            Chunk chunk = embeddingChunk.getChunk();
            float[] vector = toFloatArray(embeddingChunk.getEmbedding());
            Metadata metadata = new Metadata()
                    .put("chunkId", safe(chunk.getId()))
                    .put("chunkType", safe(chunk.getType()))
                    .put("path", safe(chunk.getPath()))
                    .put("symbol", safe(chunk.getSymbol()));
            TextSegment segment = TextSegment.from(safe(chunk.getContent()), metadata);
            String embeddingId = embeddingStore.add(Embedding.from(vector), segment);

            chunkByEmbeddingId.put(embeddingId, chunk);
            vectorByEmbeddingId.put(embeddingId, vector);
            if ("doc".equalsIgnoreCase(chunk.getType())) {
                storedDocEmbeddingIds.add(embeddingId);
            }
        }
    }

    public List<Chunk> findLowSimilarityChunks() {
        List<Chunk> lowSimilarityChunks = new ArrayList<>();
        for (String docEmbeddingId : storedDocEmbeddingIds) {
            float[] queryVector = vectorByEmbeddingId.get(docEmbeddingId);
            if (queryVector == null || queryVector.length == 0) {
                continue;
            }

            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(Embedding.from(queryVector))
                    .maxResults(DEFAULT_SEARCH_RESULTS)
                    .build();
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(request).matches();

            Double bestNeighborScore = null;
            for (EmbeddingMatch<TextSegment> match : matches) {
                if (match == null || match.embeddingId() == null) {
                    continue;
                }
                if (docEmbeddingId.equals(match.embeddingId())) {
                    continue;
                }
                bestNeighborScore = match.score();
                break;
            }

            if (bestNeighborScore == null || bestNeighborScore < LOW_SIMILARITY_THRESHOLD) {
                Chunk stale = chunkByEmbeddingId.get(docEmbeddingId);
                if (stale != null) {
                    lowSimilarityChunks.add(stale);
                }
            }
        }
        return lowSimilarityChunks;
    }

    public List<Chunk> similaritySearch(double[] queryEmbedding) {
        if (queryEmbedding == null || queryEmbedding.length == 0) {
            return List.of();
        }

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(toFloatArray(queryEmbedding)))
                .maxResults(DEFAULT_SEARCH_RESULTS)
                .build();
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(request).matches();

        List<Chunk> chunks = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : matches) {
            if (match == null || match.embeddingId() == null) {
                continue;
            }
            Chunk chunk = chunkByEmbeddingId.get(match.embeddingId());
            if (chunk != null) {
                chunks.add(chunk);
            }
        }
        return chunks;
    }

    public List<Chunk> similaritySearch(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        double[] vector = toDoubleArray(embeddingModel.embed(query).content().vector());
        return similaritySearch(vector);
    }

    private float[] toFloatArray(double[] vector) {
        float[] values = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            values[i] = (float) vector[i];
        }
        return values;
    }

    private double[] toDoubleArray(float[] vector) {
        double[] values = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            values[i] = vector[i];
        }
        return values;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
