package com.jonathan.syncprobe.service;

import com.jonathan.syncprobe.model.Chunk;
import com.jonathan.syncprobe.model.FileHealthStatus;
import com.jonathan.syncprobe.model.Score;
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

    public List<FileHealthStatus> computeScores(List<? extends Chunk> chunks, List<double[]> embeddings) {
        int chunkCount = chunks == null ? 0 : chunks.size();
        int embeddingCount = embeddings == null ? 0 : embeddings.size();
        double averageScore = chunkCount == 0 ? 0.0 : Math.min(1.0, (double) embeddingCount / chunkCount);
        SimilarityResult.HealthStatus healthStatus;
        if (chunkCount == 0 || embeddingCount == 0) {
            healthStatus = SimilarityResult.HealthStatus.STALE;
        } else if (averageScore < 0.5) {
            healthStatus = SimilarityResult.HealthStatus.AT_RISK;
        } else {
            healthStatus = SimilarityResult.HealthStatus.HEALTHY;
        }

        List<FileHealthStatus> scores = new ArrayList<>();
        scores.add(new FileHealthStatus("repository", chunkCount, averageScore, healthStatus));
        return scores;
    }

    public List<Score> computeScoreModels(List<? extends Chunk> chunks, List<double[]> embeddings) {
        List<FileHealthStatus> fileScores = computeScores(chunks, embeddings);
        List<Score> scores = new ArrayList<>();
        for (FileHealthStatus fileScore : fileScores) {
            scores.add(new Score(
                    fileScore.getFileName(),
                    fileScore.getTotalChunks(),
                    fileScore.getAverageScore(),
                    fileScore.getStatus()
            ));
        }
        return scores;
    }
}
