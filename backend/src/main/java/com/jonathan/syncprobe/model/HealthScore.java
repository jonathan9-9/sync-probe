package com.jonathan.syncprobe.model;

import java.time.Instant;

public class HealthScore {
    private String commitHash;
    private String repositoryName;

    private double overallScore;
    private double averageSimilarity; // 0 - 1

    private int totalChunks;
    private int healthyChunks;
    private int atRiskChunks;
    private int staleChunks;

    private Instant analyzedAt;

    public HealthScore(){}
    public HealthScore(String commitHash,
                       String repositoryName,
                       double overallScore,
                       double averageSimilarity,
                       int totalChunks,
                       int healthyChunks,
                       int atRiskChunks,
                       int staleChunks,
                       Instant analyzedAt
                       ) {
        this.commitHash = commitHash;
        this.repositoryName = repositoryName;
        this.overallScore = overallScore;
        this.averageSimilarity = averageSimilarity;
        this.totalChunks = totalChunks;
        this.healthyChunks = healthyChunks;
        this.atRiskChunks = atRiskChunks;
        this.staleChunks = staleChunks;
        this.analyzedAt = analyzedAt;

    }

}
