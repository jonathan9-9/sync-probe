package com.jonathan.syncprobe.service;

import com.jonathan.syncprobe.model.Chunk;
import com.jonathan.syncprobe.model.Suggestion;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

// Google AI Agent
@Service
public class SuggestionService {
    public List<Suggestion> generateFixes(List<Chunk> staleDocs) {
        List<Suggestion> suggestions = new ArrayList<>();
        if (staleDocs == null || staleDocs.isEmpty()) {
            suggestions.add(new Suggestion("No stale documentation chunks detected."));
            return suggestions;
        }

        for (Chunk staleDoc : staleDocs) {
            if (staleDoc == null) continue;
            String path = staleDoc.getPath() == null || staleDoc.getPath().isBlank() ? "unknown-file" : staleDoc.getPath();
            String chunkId = staleDoc.getId() == null || staleDoc.getId().isBlank() ? "unknown-chunk" : staleDoc.getId();
            suggestions.add(new Suggestion(
                    "Low-similarity doc chunk detected in " + path + " (" + chunkId + "). Review and update this section."
            ));
        }
        return suggestions;
    }
}
