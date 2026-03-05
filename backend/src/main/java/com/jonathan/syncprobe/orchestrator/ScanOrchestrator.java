package com.jonathan.syncprobe.orchestrator;

import com.jonathan.syncprobe.model.ScanResult;
import com.jonathan.syncprobe.service.*;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.Instant;

@Component
public class ScanOrchestrator {
    private final GitRepoIngestionService repoIngestionService;
    private final DocIngestionService docIngestionService;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final SimilarityService similarityService;
    private final SuggestionService suggestionService;

    public ScanOrchestrator(GitRepoIngestionService repoIngestionService,
                            DocIngestionService docIngestionService,
                            ChunkingService chunkingService,
                            EmbeddingService embeddingService,
                            SimilarityService similarityService,
                            SuggestionService suggestionService
                            ){
        this.repoIngestionService = repoIngestionService;
        this.docIngestionService = docIngestionService;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.similarityService = similarityService;
        this.suggestionService = suggestionService;
    }
    public ScanResult runScan(String repoUrl){
        Path repoPath = repoIngestionService.ingestRepository(repoUrl);

        var docs = docIngestionService.parseDocs(repoPath);
        var chunks = chunkingService.chunkAll(docs);
        var embeddings = embeddingService.embedChunks(chunks);
        var scores = similarityService.computeScores(chunks, embeddings);
        var suggestions = suggestionService.generateFixes(scores);

        String repoName = repoPath.getFileName() == null ? "" : repoPath.getFileName().toString();
        return new ScanResult(
                repoName,
                repoPath.toString(),
                null,
                Instant.now(),
                scores,
                suggestions
        );
    }
}
