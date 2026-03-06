package com.jonathan.syncprobe.orchestrator;

import com.jonathan.syncprobe.model.*;
import com.jonathan.syncprobe.service.ChunkingService;
import com.jonathan.syncprobe.service.EmbeddingService;
import com.jonathan.syncprobe.service.GitRepoIngestionService;
import com.jonathan.syncprobe.service.MarkdownDocIngestionService;
import com.jonathan.syncprobe.service.SimilarityService;
import com.jonathan.syncprobe.service.SuggestionService;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScanOrchestrator {

    private final GitRepoIngestionService repoIngestionService;
    private final MarkdownDocIngestionService markdownDocIngestionService;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final SimilarityService similarityService;
    private final SuggestionService suggestionService;

    public ScanOrchestrator(
            GitRepoIngestionService repoIngestionService,
            MarkdownDocIngestionService markdownDocIngestionService,
            ChunkingService chunkingService,
            EmbeddingService embeddingService,
            SimilarityService similarityService,
            SuggestionService suggestionService
    ) {
        this.repoIngestionService = repoIngestionService;
        this.markdownDocIngestionService = markdownDocIngestionService;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.similarityService = similarityService;
        this.suggestionService = suggestionService;
    }

    public ScanResult runScan(String repoUrl) {

        // 1. Clone repo
        Path repoPath = repoIngestionService.ingestRepository(repoUrl);

        // 2. Parse markdown docs
        List<DocChunk> docs = markdownDocIngestionService.parseDocs(repoPath);

        // parseDocs already returns chunked docs, and DocChunk extends Chunk.
        List<Chunk> chunks = new ArrayList<>(docs);

        // 4. Generate embeddings
        List<EmbeddingChunk> embeddings = embeddingService.embedChunks(chunks);

        // 5. Compute similarity scores
        List<double[]> vectors = embeddings.stream()
                .map(EmbeddingChunk::getEmbedding)
                .collect(Collectors.toList());
        List<FileHealthStatus> scores = similarityService.computeScores(chunks, vectors);

        // 6. Generate suggestions
        List<Suggestion> suggestions = suggestionService.generateFixes(scores);

        String repoName =
                repoPath.getFileName() == null
                        ? ""
                        : repoPath.getFileName().toString();

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
