package com.jonathan.syncprobe.service;

import com.jonathan.syncprobe.model.DocChunk;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

// Parse markdown docs
@Service
public class DocIngestionService {
    public DocIngestionService(){}

    public List<DocChunk> parseDocs(Path repoPath) {
        List<DocChunk> docs = new ArrayList<>();
        if (repoPath == null || !Files.exists(repoPath)) {
            return docs;
        }

        try (Stream<Path> paths = Files.walk(repoPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".md"))
                    .forEach(path -> docs.add(new DocChunk()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse docs from repository: " + repoPath, e);
        }

        return docs;
    }
}
