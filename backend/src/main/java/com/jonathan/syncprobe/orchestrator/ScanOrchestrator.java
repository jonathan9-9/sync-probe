package com.jonathan.syncprobe.orchestrator;

import com.jonathan.syncprobe.model.*;
import com.jonathan.syncprobe.service.ChunkingService;
import com.jonathan.syncprobe.service.CodeIngestionService;
import com.jonathan.syncprobe.service.EmbeddingService;
import com.jonathan.syncprobe.service.GitRepoIngestionService;
import com.jonathan.syncprobe.service.MarkdownDocIngestionService;
import com.jonathan.syncprobe.service.SuggestionService;
import com.jonathan.syncprobe.vector.VectorStoreService;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScanOrchestrator {

    private final GitRepoIngestionService repoIngestionService;
    private final MarkdownDocIngestionService markdownDocIngestionService;
    private final CodeIngestionService codeIngestionService;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final SuggestionService suggestionService;

    public ScanOrchestrator(
            GitRepoIngestionService repoIngestionService,
            MarkdownDocIngestionService markdownDocIngestionService,
            CodeIngestionService codeIngestionService,
            ChunkingService chunkingService,
            EmbeddingService embeddingService,
            VectorStoreService vectorStoreService,
            SuggestionService suggestionService
    ) {
        this.repoIngestionService = repoIngestionService;
        this.markdownDocIngestionService = markdownDocIngestionService;
        this.codeIngestionService = codeIngestionService;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.suggestionService = suggestionService;
    }

    public ScanResult runScan(String repoUrl) {
        return runScan("default-scan", repoUrl);
    }

    public ScanResult runScan(String scanId, String repoUrl) {

        // 1. Clone repo
        Path repoPath = repoIngestionService.ingestRepository(repoUrl);

        // 2. Parse markdown docs
        List<DocChunk> docs = markdownDocIngestionService.parseDocs(repoPath);
        List<CodeChunk> codeChunks = codeIngestionService.parseCode(repoPath);

        List<Chunk> chunks = new ArrayList<>();
        chunks.addAll(chunkingService.chunkDocuments(docs));
        chunks.addAll(codeChunks);

        List<EmbeddingChunk> embeddings = embeddingService.embedChunks(chunks);

        vectorStoreService.store(scanId, embeddings);

        List<Chunk> staleDocs = vectorStoreService.findLowSimilarityChunks(scanId);
        List<Chunk> relatedCodeChunks = vectorStoreService.findRelatedCodeChunks(scanId, staleDocs, 3);

        List<Suggestion> suggestions = suggestionService.generateFixes(staleDocs, relatedCodeChunks);

        return new ScanResult(suggestions);
    }

    /**
     * to avoid full repo scan we instead run runQuery to retrieve relevant chunks
     * */
    public List<Suggestion> runQuery(String query) {
        double[] queryEmbedding = embeddingService.embedText(query);
        List<Chunk> relevantChunks = vectorStoreService.similaritySearch(queryEmbedding);
        return suggestionService.generateFixes(relevantChunks, relevantChunks);
    }
}
