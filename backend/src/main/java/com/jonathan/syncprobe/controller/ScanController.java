package com.jonathan.syncprobe.controller;

import com.jonathan.syncprobe.controller.dto.ScanRequestDto;
import com.jonathan.syncprobe.controller.dto.ScanStartResponseDto;
import com.jonathan.syncprobe.controller.dto.ScanStatusResponseDto;
import com.jonathan.syncprobe.model.ScanJobStatus;
import com.jonathan.syncprobe.model.ScanResult;
import com.jonathan.syncprobe.service.ScanJobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scans")
@Validated
public class ScanController {

    private final ScanJobService scanJobService;

    public ScanController(ScanJobService scanJobService) {
        this.scanJobService = scanJobService;
    }

    @PostMapping
    public ResponseEntity<ScanStartResponseDto> startScan(@Valid @RequestBody ScanRequestDto request) {
        String id = scanJobService.submit(request.repoUrl);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ScanStartResponseDto(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScanStatusResponseDto> getStatus(@PathVariable String id) {
        ScanJobStatus status = scanJobService.getStatus(id);
        if (status == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        ScanResult result = scanJobService.getResult(id);
        String error = scanJobService.getError(id);
        return ResponseEntity.ok(new ScanStatusResponseDto(status, result, error));
    }
}
