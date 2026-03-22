package com.jonathan.syncprobe.service;

import com.jonathan.syncprobe.model.ScanJobStatus;
import com.jonathan.syncprobe.model.ScanResult;
import com.jonathan.syncprobe.orchestrator.ScanOrchestrator;
import com.jonathan.syncprobe.persistence.entity.ScanRecord;
import com.jonathan.syncprobe.persistence.repository.ScanRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ScanJobService {

    private final ScanOrchestrator scanOrchestrator;
    private final Map<String, ScanJobStatus> statusById = new ConcurrentHashMap<>();
    private final Map<String, ScanResult> resultById = new ConcurrentHashMap<>();
    private final Map<String, String> errorById = new ConcurrentHashMap<>();
    private final ScanRecordRepository scanRecordRepository;
    private final ObjectMapper objectMapper;

    public ScanJobService(ScanOrchestrator scanOrchestrator,
                          ScanRecordRepository scanRecordRepository,
                          ObjectMapper objectMapper) {
        this.scanOrchestrator = scanOrchestrator;
        this.scanRecordRepository = scanRecordRepository;
        this.objectMapper = objectMapper;
    }

    public String submit(String repoUrl) {
        String id = UUID.randomUUID().toString();
        statusById.put(id, ScanJobStatus.RUNNING);
        ScanRecord record = new ScanRecord();
        record.setId(id);
        record.setRepoUrl(repoUrl);
        record.setStatus(ScanJobStatus.RUNNING);
        scanRecordRepository.save(record);
        runAsync(id, repoUrl);
        return id;
    }

    @Async("scanExecutor")
    protected CompletableFuture<Void> runAsync(String id, String repoUrl) {
        try {
            ScanResult result = scanOrchestrator.runScan(id, repoUrl);
            resultById.put(id, result);
            statusById.put(id, ScanJobStatus.SUCCEEDED);

            persistSuccess(id, result);
        } catch (Exception ex) {
            errorById.put(id, ex.getMessage());
            statusById.put(id, ScanJobStatus.FAILED);
            persistFailure(id, ex.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    public ScanJobStatus getStatus(String id) {
        ScanJobStatus status = statusById.get(id);
        if (status != null) {
            return status;
        }
        return scanRecordRepository.findById(id)
                .map(ScanRecord::getStatus)
                .orElse(null);
    }

    public ScanResult getResult(String id) {
        ScanResult result = resultById.get(id);
        if (result != null) {
            return result;
        }
        return scanRecordRepository.findById(id)
                .map(ScanRecord::getResultJson)
                .map(this::deserializeResult)
                .orElse(null);
    }

    public String getError(String id) {
        String error = errorById.get(id);
        if (error != null) {
            return error;
        }
        return scanRecordRepository.findById(id)
                .map(ScanRecord::getError)
                .orElse(null);
    }

    private void persistSuccess(String id, ScanResult result) {
        scanRecordRepository.findById(id).ifPresent(record -> {
            record.setStatus(ScanJobStatus.SUCCEEDED);
            record.setFinishedAt(java.time.Instant.now());
            try {
                record.setResultJson(objectMapper.writeValueAsString(result));
            } catch (JsonProcessingException e) {
                record.setError("Failed to serialize result: " + e.getMessage());
            }
            scanRecordRepository.save(record);
        });
    }

    private void persistFailure(String id, String error) {
        scanRecordRepository.findById(id).ifPresent(record -> {
            record.setStatus(ScanJobStatus.FAILED);
            record.setFinishedAt(java.time.Instant.now());
            record.setError(error);
            scanRecordRepository.save(record);
        });
    }

    private ScanResult deserializeResult(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ScanResult.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
