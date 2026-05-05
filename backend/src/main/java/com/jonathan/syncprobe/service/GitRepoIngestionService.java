package com.jonathan.syncprobe.service;

import com.jonathan.syncprobe.utils.GitUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

// Clone and parse repo
@Service
public class GitRepoIngestionService implements RepoIngestionService {
    private static final Logger log = LoggerFactory.getLogger(GitRepoIngestionService.class);

    @Override
    public Path ingestRepository(String repoUrl){
        try {
            // Clone into an ephemeral workspace for this scan only.
            Path targetDir = Files.createTempDirectory("syncprobe-repo-");
            GitUtils.cloneRepo(targetDir, repoUrl);
            return targetDir;
        } catch (IOException e){
            throw new RuntimeException("Failed to clone repository", e);
        }
    }

    @Override
    public void cleanupRepository(Path repoPath) {
        if (repoPath == null || !Files.exists(repoPath)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(repoPath)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            log.warn("Failed to delete temporary repo path: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to cleanup temporary repository directory: {}", repoPath, e);
        }
    }
}
