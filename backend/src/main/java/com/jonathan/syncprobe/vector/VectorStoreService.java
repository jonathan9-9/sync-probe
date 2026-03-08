package com.jonathan.syncprobe.vector;

import com.jonathan.syncprobe.model.Chunk;
import com.jonathan.syncprobe.model.EmbeddingChunk;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VectorStoreService {
    private static final int DEFAULT_SEARCH_RESULTS = 8;
    private static final double LOW_SIMILARITY_THRESHOLD = 0.55;

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final Map<String, Chunk> chunkByEmbeddingId = new HashMap<>();
    private final Map<String, String> embeddingIdByChunkId = new HashMap<>();
    private final Map<String, float[]> vectorByEmbeddingId = new HashMap<>();
    private final List<String> storedDocEmbeddingIds = new ArrayList<>();

    public VectorStoreService(EmbeddingStore<TextSegment> embeddingStore){
        this.embeddingStore = embeddingStore;
    }

    public void store(List<EmbeddingChunk> embeddings) {
        chunkByEmbeddingId.clear();
        embeddingIdByChunkId.clear();
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
            if (chunk.getId() != null && !chunk.getId().isBlank()) {
                embeddingIdByChunkId.put(chunk.getId(), embeddingId);
            }
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
            if (isEmptyVector(queryVector)) {
                continue;
            }

            List<EmbeddingMatch<TextSegment>> matches = searchCodeMatches(queryVector, DEFAULT_SEARCH_RESULTS);

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

    public List<Chunk> findRelatedCodeChunks(List<Chunk> docChunks, int maxResultsPerDoc) {
        if (docChunks == null || docChunks.isEmpty()) {
            return List.of();
        }

        int maxResults = maxResultsPerDoc <= 0 ? 3 : maxResultsPerDoc;
        Map<String, Chunk> unique = new LinkedHashMap<>();
        for (Chunk docChunk : docChunks) {
            if (docChunk == null || docChunk.getId() == null) {
                continue;
            }
            String embeddingId = embeddingIdByChunkId.get(docChunk.getId());
            if (embeddingId == null) {
                continue;
            }
            float[] docVector = vectorByEmbeddingId.get(embeddingId);
            if (isEmptyVector(docVector)) {
                continue;
            }

            List<EmbeddingMatch<TextSegment>> matches = searchCodeMatches(docVector, maxResults);
            for (EmbeddingMatch<TextSegment> match : matches) {
                if (match == null || match.embeddingId() == null) {
                    continue;
                }
                Chunk chunk = chunkByEmbeddingId.get(match.embeddingId());
                if (chunk != null) {
                    String key = chunk.getId() == null ? match.embeddingId() : chunk.getId();
                    unique.putIfAbsent(key, chunk);
                }
            }
        }
        return new ArrayList<>(unique.values());
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

    private float[] toFloatArray(double[] vector) {
        float[] values = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            values[i] = (float) vector[i];
        }
        return values;
    }

    private boolean isEmptyVector(float[] vector) {
        return vector == null || vector.length == 0;
    }

    private List<EmbeddingMatch<TextSegment>> searchCodeMatches(float[] queryVector, int maxResults) {
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(queryVector))
                .maxResults(maxResults)
                .filter(new IsEqualTo("chunkType", "code"))
                .build();
        return embeddingStore.search(request).matches();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
