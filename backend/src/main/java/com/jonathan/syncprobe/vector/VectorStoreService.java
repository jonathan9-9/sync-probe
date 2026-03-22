package com.jonathan.syncprobe.vector;

import com.jonathan.syncprobe.model.Chunk;
import com.jonathan.syncprobe.model.EmbeddingChunk;
import com.jonathan.syncprobe.persistence.entity.ChunkEmbeddingRecord;
import com.jonathan.syncprobe.persistence.repository.ChunkEmbeddingRepository;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.StringJoiner;

@Service
public class VectorStoreService {
    private static final int DEFAULT_SEARCH_RESULTS = 8;
    private static final double LOW_SIMILARITY_THRESHOLD = 0.55;

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ChunkEmbeddingRepository chunkEmbeddingRepository;
    // keyed by scanId to isolate multi-user scans
    private final Map<String, Map<String, Chunk>> chunkByEmbeddingIdByScan = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> embeddingIdByChunkIdByScan = new ConcurrentHashMap<>();
    private final Map<String, Map<String, float[]>> vectorByEmbeddingIdByScan = new ConcurrentHashMap<>();
    private final Map<String, List<String>> storedDocEmbeddingIdsByScan = new ConcurrentHashMap<>();

    public VectorStoreService(EmbeddingStore<TextSegment> embeddingStore,
                              ChunkEmbeddingRepository chunkEmbeddingRepository){
        this.embeddingStore = embeddingStore;
        this.chunkEmbeddingRepository = chunkEmbeddingRepository;
    }

    public void store(String scanId, List<EmbeddingChunk> embeddings) {
        if (embeddings == null || embeddings.isEmpty()) {
            return;
        }

        Map<String, Chunk> chunkByEmbeddingId = chunkByEmbeddingIdByScan.computeIfAbsent(scanId, k -> new ConcurrentHashMap<>());
        Map<String, String> embeddingIdByChunkId = embeddingIdByChunkIdByScan.computeIfAbsent(scanId, k -> new ConcurrentHashMap<>());
        Map<String, float[]> vectorByEmbeddingId = vectorByEmbeddingIdByScan.computeIfAbsent(scanId, k -> new ConcurrentHashMap<>());
        List<String> storedDocEmbeddingIds = storedDocEmbeddingIdsByScan.computeIfAbsent(scanId, k -> new CopyOnWriteArrayList<>());

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

            ChunkEmbeddingRecord record = new ChunkEmbeddingRecord();
            record.setScanId(scanId);
            record.setChunkId(chunk.getId());
            record.setChunkType(chunk.getType());
            record.setPath(chunk.getPath());
            record.setSymbol(chunk.getSymbol());
            record.setContent(chunk.getContent());
            record.setEmbeddingId(embeddingId);
            record.setVector(vectorToString(vector));
            chunkEmbeddingRepository.save(record);
        }
    }

    public List<Chunk> findLowSimilarityChunks(String scanId) {
        loadCacheFromDbIfMissing(scanId);

        Map<String, float[]> vectorByEmbeddingId = vectorByEmbeddingIdByScan.get(scanId);
        List<String> storedDocEmbeddingIds = storedDocEmbeddingIdsByScan.get(scanId);
        Map<String, Chunk> chunkByEmbeddingId = chunkByEmbeddingIdByScan.get(scanId);
        if (vectorByEmbeddingId == null || storedDocEmbeddingIds == null || chunkByEmbeddingId == null) {
            return List.of();
        }
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

    public List<Chunk> findRelatedCodeChunks(String scanId, List<Chunk> docChunks, int maxResultsPerDoc) {
        loadCacheFromDbIfMissing(scanId);

        Map<String, float[]> vectorByEmbeddingId = vectorByEmbeddingIdByScan.get(scanId);
        Map<String, String> embeddingIdByChunkId = embeddingIdByChunkIdByScan.get(scanId);
        Map<String, Chunk> chunkByEmbeddingId = chunkByEmbeddingIdByScan.get(scanId);
        if (docChunks == null || docChunks.isEmpty() || vectorByEmbeddingId == null || embeddingIdByChunkId == null || chunkByEmbeddingId == null) {
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

        if (chunkByEmbeddingIdByScan.isEmpty()) {
            loadAllCachesFromDb();
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
            // When scanning multiple scans, embedding IDs are not globally unique per scan. We search all scans.
            chunkByEmbeddingIdByScan.values().forEach(map -> {
                Chunk chunk = map.get(match.embeddingId());
                if (chunk != null) {
                    chunks.add(chunk);
                }
            });
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

    private String vectorToString(float[] vector) {
        if (vector == null || vector.length == 0) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(",");
        for (float v : vector) {
            joiner.add(Float.toString(v));
        }
        return joiner.toString();
    }

    private float[] stringToFloatArray(String vector) {
        if (vector == null || vector.isBlank()) {
            return new float[0];
        }
        String[] parts = vector.split(",");
        float[] values = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                values[i] = Float.parseFloat(parts[i]);
            } catch (NumberFormatException ex) {
                values[i] = 0f;
            }
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

    private void loadCacheFromDbIfMissing(String scanId) {
        if (chunkByEmbeddingIdByScan.containsKey(scanId)) {
            return;
        }
        List<ChunkEmbeddingRecord> records = chunkEmbeddingRepository.findByScanId(scanId);
        if (records.isEmpty()) {
            return;
        }
        for (ChunkEmbeddingRecord record : records) {
            cacheRecord(record);
        }
    }

    private void loadAllCachesFromDb() {
        chunkEmbeddingRepository.findAll().forEach(this::cacheRecord);
    }

    private void cacheRecord(ChunkEmbeddingRecord record) {
        String scanId = record.getScanId();
        Map<String, Chunk> chunkByEmbeddingId = chunkByEmbeddingIdByScan.computeIfAbsent(scanId, k -> new ConcurrentHashMap<>());
        Map<String, String> embeddingIdByChunkId = embeddingIdByChunkIdByScan.computeIfAbsent(scanId, k -> new ConcurrentHashMap<>());
        Map<String, float[]> vectorByEmbeddingId = vectorByEmbeddingIdByScan.computeIfAbsent(scanId, k -> new ConcurrentHashMap<>());
        List<String> storedDocEmbeddingIds = storedDocEmbeddingIdsByScan.computeIfAbsent(scanId, k -> new CopyOnWriteArrayList<>());

        Chunk chunk = new Chunk(record.getChunkId(), record.getContent(), record.getChunkType(), record.getSymbol(), record.getPath());
        chunkByEmbeddingId.put(record.getEmbeddingId(), chunk);
        if (chunk.getId() != null && !chunk.getId().isBlank()) {
            embeddingIdByChunkId.put(chunk.getId(), record.getEmbeddingId());
        }
        float[] vector = stringToFloatArray(record.getVector());
        vectorByEmbeddingId.put(record.getEmbeddingId(), vector);
        if ("doc".equalsIgnoreCase(chunk.getType())) {
            storedDocEmbeddingIds.add(record.getEmbeddingId());
        }
    }
}
