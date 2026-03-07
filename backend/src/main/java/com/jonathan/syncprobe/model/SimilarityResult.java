package com.jonathan.syncprobe.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class SimilarityResult {
    private String id;

    // doc metadata
    private String docChunkId;
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
        SYNCING
    }

    @Getter
    @Setter
    public static class RelatedMatch{
        private String codePath;
        private String codeSymbol; // method or class name
        private String codeChunkId;
        private double score;

        public RelatedMatch(){}

        public RelatedMatch(String codePath, String codeSymbol, String codeChunkId, double score){
            this.codePath = codePath;
            this.codeSymbol = codeSymbol;
            this.codeChunkId = codeChunkId;
            this.score = score;
        }
    }
    public SimilarityResult(String id,
                            String docChunkId,
                            String docPath,
                            String docSectionTitle,
                            String codePath,
                            String codeSymbol,
                            String codeChunkId,
                            double similarityScore,
                            HealthStatus status,
                            Instant analyzedAt
                            ){
        this.id = id;
        this.docChunkId = docChunkId;
        this.docPath = docPath;
        this.docSectionTitle = docSectionTitle;
        this.codeSymbol = codeSymbol;
        this.codePath = codePath;
        this.codeChunkId = codeChunkId;
        this.similarityScore = similarityScore;
        this.status = status;
        this.analyzedAt = analyzedAt;
    }
}
