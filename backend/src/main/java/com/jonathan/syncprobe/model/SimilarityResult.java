package com.jonathan.syncprobe.model;

import java.time.Instant;
import java.util.List;

public class SimilarityResult {
    private String id;

    // doc metadata
    private String chunkId;
    private String docPath;
    private String docSectionTitle;

    // matched code metadata
    private String codePath;
    private String codeSymbol; // method or class name
    private String codeChunkId;

    private double similarityScore; // 0-1.0 metric
    private HealthStatus status;

    private List<RelatedMatch> topMatches;

    private Instant analyzedAt;
    public enum HealthStatus {
        HEALTHY,
        AT_RISK,
        STALE,

    }

    public static class RelatedMatch{
        private String codePath;
        private String codeSymbol; // method or class name
        private String codeChunkId;
        private double score;

        public RelatedMatch(){}
    }

}

