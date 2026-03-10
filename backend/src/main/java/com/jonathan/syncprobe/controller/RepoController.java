package com.jonathan.syncprobe.controller;

import com.jonathan.syncprobe.model.ScanResult;
import com.jonathan.syncprobe.model.Suggestion;
import com.jonathan.syncprobe.orchestrator.ScanOrchestrator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Accepts repository url and triggers the analysis of source code files and doc
 */
@RestController
@RequestMapping("/api/scan")
public class RepoController {
    private final ScanOrchestrator scanOrchestrator;
    public RepoController(ScanOrchestrator scanOrchestrator){
        this.scanOrchestrator = scanOrchestrator;
    }

    @PostMapping
    public ResponseEntity<ScanResult> scanRepository(@RequestBody ScanRequest request){
        if (request == null || request.repoUrl == null || request.repoUrl.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        ScanResult result = scanOrchestrator.runScan(request.repoUrl.trim());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/query")
    public ResponseEntity<SuggestionResponse> query(@RequestBody QueryRequest request){
        if (request == null || request.query == null || request.query.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        var suggestions = scanOrchestrator.runQuery(request.query.trim());
        return ResponseEntity.ok(new SuggestionResponse(suggestions));
    }

    public static final class ScanRequest {
        public String repoUrl;

        public ScanRequest() {}

        public ScanRequest(String repoUrl) {
            this.repoUrl = repoUrl;
        }
    }

    public static final class QueryRequest {
        public String query;
        public QueryRequest() {}
        public QueryRequest(String query) { this.query = query; }
    }

    public static final class SuggestionResponse {
        public List<com.jonathan.syncprobe.model.Suggestion> suggestions;
        public SuggestionResponse(List<com.jonathan.syncprobe.model.Suggestion> suggestions) {
            this.suggestions = suggestions;
        }
    }
}
