package com.jonathan.syncprobe.controller;

import com.jonathan.syncprobe.controller.dto.QueryRequestDto;
import com.jonathan.syncprobe.controller.dto.ScanRequestDto;
import com.jonathan.syncprobe.controller.dto.SuggestionResponseDto;
import com.jonathan.syncprobe.model.ScanResult;
import com.jonathan.syncprobe.orchestrator.ScanOrchestrator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Accepts repository url and triggers the analysis of source code files and doc
 */
@RestController
@RequestMapping("/api/scan")
@Validated
public class RepoController {
    private final ScanOrchestrator scanOrchestrator;
    public RepoController(ScanOrchestrator scanOrchestrator){
        this.scanOrchestrator = scanOrchestrator;
    }

    @PostMapping
    public ResponseEntity<ScanResult> scanRepository(@Valid @RequestBody ScanRequestDto request){
        if (request == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        ScanResult result = scanOrchestrator.runScan(request.repoUrl.trim());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/query")
    public ResponseEntity<SuggestionResponseDto> query(@Valid @RequestBody QueryRequestDto request){
        if (request == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        var suggestions = scanOrchestrator.runQuery(request.query.trim());
        return ResponseEntity.ok(new SuggestionResponseDto(suggestions));
    }
}
