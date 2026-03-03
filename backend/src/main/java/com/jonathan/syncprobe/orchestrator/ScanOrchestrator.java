package com.jonathan.syncprobe.orchestrator;

import com.jonathan.syncprobe.model.ScanResult;
import com.jonathan.syncprobe.service.*;
import org.springframework.stereotype.Component;

@Component
public class ScanOrchestrator {
    private final RepoIngestionService repoIngestionService;
    private final DocIngestionService docIngestionService;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final SimilarityService similarityService;
    private final SuggestionService suggestionService;

    public ScanOrchestrator(RepoIngestionService repoIngestionService,
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
        return null;
    }
}
