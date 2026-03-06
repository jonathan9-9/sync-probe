package com.jonathan.syncprobe.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Score {
    private String fileName;
    private int totalChunks;
    private double averageScore;
    private SimilarityResult.HealthStatus status;

    public Score() {}

    public Score(String fileName, int totalChunks, double averageScore, SimilarityResult.HealthStatus status) {
        this.fileName = fileName;
        this.totalChunks = totalChunks;
        this.averageScore = averageScore;
        this.status = status;
    }
}
