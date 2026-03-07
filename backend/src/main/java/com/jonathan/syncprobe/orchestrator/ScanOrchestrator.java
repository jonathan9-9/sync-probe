package com.jonathan.syncprobe.orchestrator;

import com.jonathan.syncprobe.model.*;
import com.jonathan.syncprobe.service.ChunkingService;
import com.jonathan.syncprobe.service.EmbeddingService;
import com.jonathan.syncprobe.service.GitRepoIngestionService;
import com.jonathan.syncprobe.service.MarkdownDocIngestionService;
import com.jonathan.syncprobe.service.SuggestionService;
import com.jonathan.syncprobe.vector.VectorStoreService;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class ScanOrchestrator {

    private final GitRepoIngestionService repoIngestionService;
    private final MarkdownDocIngestionService markdownDocIngestionService;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final SuggestionService suggestionService;

    public ScanOrchestrator(
            GitRepoIngestionService repoIngestionService,
            MarkdownDocIngestionService markdownDocIngestionService,
            ChunkingService chunkingService,
            EmbeddingService embeddingService,
            VectorStoreService vectorStoreService,
            SuggestionService suggestionService
    ) {
        this.repoIngestionService = repoIngestionService;
        this.markdownDocIngestionService = markdownDocIngestionService;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.suggestionService = suggestionService;
    }

    public ScanResult runScan(String repoUrl) {

        // 1. Clone repo
        Path repoPath = repoIngestionService.ingestRepository(repoUrl);

        // 2. Parse markdown docs
        List<DocChunk> docs = markdownDocIngestionService.parseDocs(repoPath);

        List<Chunk> chunks = chunkingService.chunkDocuments(docs);

        List<EmbeddingChunk> embeddings = embeddingService.embedChunks(chunks);

        vectorStoreService.store(embeddings);

        List<Chunk> staleDocs = vectorStoreService.findLowSimilarityChunks();

        List<Suggestion> suggestions = suggestionService.generateFixes(staleDocs);

        return new ScanResult(suggestions);
    }

    public List<Suggestion> runQuery(String query) {
        double[] queryEmbedding = embeddingService.embedText(query);
        List<Chunk> relevantChunks = vectorStoreService.similaritySearch(queryEmbedding);
        return suggestionService.generateFixes(relevantChunks);
    }
}
