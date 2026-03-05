package com.jonathan.syncprobe.service;

import com.jonathan.syncprobe.model.FileHealthStatus;
import com.jonathan.syncprobe.model.SimilarityResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

// Google AI Agent
@Service
public class SuggestionService {
    public List<String> generateFixes(List<FileHealthStatus> scores) {
        List<String> suggestions = new ArrayList<>();
        if (scores == null || scores.isEmpty()) {
            suggestions.add("No scores available. Ensure docs were parsed and chunked correctly.");
            return suggestions;
        }

        for (FileHealthStatus score : scores) {
            if (score.getStatus() == SimilarityResult.HealthStatus.STALE) {
                suggestions.add("Documentation appears stale. Update docs to match recent code behavior.");
            } else if (score.getStatus() == SimilarityResult.HealthStatus.AT_RISK) {
                suggestions.add("Review low-similarity sections and align docs with implementation details.");
            }
        }
        return suggestions;
    }
}
