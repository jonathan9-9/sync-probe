package com.jonathan.syncprobe.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Setter
@Getter
public class ScanResult {
    private String repositoryName;
    private String repoPath;
    private String commitHash;
    private Instant scannedAt;
    private List<Suggestion> proposedFixes;

    public ScanResult(){}
    public ScanResult(List<Suggestion> proposedFixes) {
        this.scannedAt = Instant.now();
        this.proposedFixes = proposedFixes;
    }

    public ScanResult(String repositoryName,
                      String repoPath,
                      String commitHash,
                      Instant scannedAt,
                      List<Suggestion> proposedFixes
                      ) {
        this.repositoryName = repositoryName;
        this.repoPath = repoPath;
        this.commitHash = commitHash;
        this.scannedAt = scannedAt;
        this.proposedFixes = proposedFixes;
    }


}
