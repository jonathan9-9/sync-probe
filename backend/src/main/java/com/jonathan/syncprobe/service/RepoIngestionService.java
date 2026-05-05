package com.jonathan.syncprobe.service;

import java.nio.file.Path;

public interface RepoIngestionService {
    Path ingestRepository(String repoUrl);
    void cleanupRepository(Path repoPath);
}
