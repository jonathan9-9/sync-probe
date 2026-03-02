package com.jonathan.syncprobe.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

public class ScanResult {
    private String repositoryName;
    @Setter
    @Getter
    private String repoPath;
    private String commitHash;
    private Instant scannedAt;
    private List<HealthScore> files;
    private List<String> proposedFixes;

    public ScanResult(){}
    public ScanResult(String repositoryName,
                      String repoPath,
                      String commitHash,
                      Instant scannedAt,
                      List<HealthScore> files,
                      List<String> proposedFixes
                      ) {
        this.repositoryName = repositoryName;
        this.repoPath = repoPath;
        this.commitHash = commitHash;
        this.scannedAt = scannedAt;
        this.files = files;
        this.proposedFixes = proposedFixes;
    }

}
