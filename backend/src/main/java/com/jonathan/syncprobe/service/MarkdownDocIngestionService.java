package com.jonathan.syncprobe.service;

import com.jonathan.syncprobe.model.DocChunk;
import com.jonathan.syncprobe.utils.MarkdownUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class MarkdownDocIngestionService {

    public List<DocChunk> parseDocs(Path repoPath) {
        List<DocChunk> docs = new ArrayList<>();

        if (repoPath == null || !Files.exists(repoPath)) return docs;

        try (Stream<Path> paths = Files.walk(repoPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::isDocumentationFile)
                    .forEach(file -> {
                        try {
                            String markdown = Files.readString(file);
                            String cleanedText = MarkdownUtils.stripMarkdown(markdown);
                            String docId = file + "-doc";
                            docs.add(new DocChunk(file.toString(), docId, cleanedText));

                            List<String> codeBlocks = MarkdownUtils.extractCodeBlocks(markdown);
                            for (int i = 0; i < codeBlocks.size(); i++) {
                                String block = codeBlocks.get(i).trim();
                                if (block.isEmpty()) continue;
                                DocChunk exampleChunk = new DocChunk(file.toString(),
                                        file + "code-example" + i,
                                        "[DOC-CODE-EXAMPLE]\n" + block
                                        );
                                exampleChunk.setSymbol("doc_code_block" + i);
                                docs.add(exampleChunk);
                            }

                        } catch (IOException e) {
                            throw new RuntimeException("Failed to read doc file: " + file, e);
                        }
                    });

        } catch (IOException e) {
            throw new RuntimeException("Failed to scan repository at path: " + repoPath, e);
        }

        return docs;
    }

    private boolean isDocumentationFile(Path file) {
        String name = file.toString().toLowerCase();
        return name.endsWith(".md")
                || name.endsWith(".markdown")
                || name.endsWith(".txt");
    }
}
