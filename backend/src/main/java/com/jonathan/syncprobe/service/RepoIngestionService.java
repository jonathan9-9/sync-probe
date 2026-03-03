package com.jonathan.syncprobe.service;

import com.jonathan.syncprobe.utils.GitUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// Clone and parse repo
@Service
public class RepoIngestionService {
    private final Path baseCloneDir = Paths.get("repos");

    public Path cloneRepo(String repoUrl){
        try {
            if (!Files.exists(baseCloneDir)){
                Files.createDirectories(baseCloneDir);
            }
            String repoName = extractRepoName(repoUrl);
            Path targetDir = baseCloneDir.resolve(repoName);
            if (Files.exists(targetDir) && GitUtils.isGitRepository(targetDir)){
                GitUtils.pullLatest(targetDir);
            } else {
                GitUtils.cloneRepo(targetDir, repoUrl);
            }
            return targetDir;
        } catch (IOException e){
            throw new RuntimeException("Failed to clone repository", e);
        }
    }

    public String extractRepoName(String repoUrl){
        String[] parts = repoUrl.split("/");
        String lastPart = parts[parts.length - 1];
        return lastPart.replace(".git", "");
    }

}
