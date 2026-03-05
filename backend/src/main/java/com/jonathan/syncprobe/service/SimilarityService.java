package com.jonathan.syncprobe.service;

import com.jonathan.syncprobe.model.Chunk;
import com.jonathan.syncprobe.model.FileHealthStatus;
import com.jonathan.syncprobe.model.SimilarityResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

// computes cosine similarity and scoring
@Service
public class SimilarityService {
    private final FileLoader fileLoader;

    public SimilarityService(@Qualifier("localProvider") FileLoader fileLoader) {
        this.fileLoader = fileLoader;
    }

    public void analyze(String codePath, String docPath) {
        String code = fileLoader.loadContent(codePath);
        String doc = fileLoader.loadContent(docPath);
        // AI logic here including cosine similarity...
    }

    public List<FileHealthStatus> computeScores(List<Chunk> chunks, List<double[]> embeddings) {
        int chunkCount = chunks == null ? 0 : chunks.size();
        double averageScore = chunkCount == 0 ? 0.0 : 1.0;
        SimilarityResult.HealthStatus healthStatus =
                chunkCount == 0 ? SimilarityResult.HealthStatus.STALE : SimilarityResult.HealthStatus.HEALTHY;

        List<FileHealthStatus> scores = new ArrayList<>();
        scores.add(new FileHealthStatus("repository", chunkCount, averageScore, healthStatus));
        return scores;
    }
}
