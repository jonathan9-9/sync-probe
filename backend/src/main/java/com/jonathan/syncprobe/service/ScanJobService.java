package com.jonathan.syncprobe.service;

import com.jonathan.syncprobe.model.ScanJobStatus;
import com.jonathan.syncprobe.model.ScanResult;
import com.jonathan.syncprobe.orchestrator.ScanOrchestrator;
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

    public ScanJobService(ScanOrchestrator scanOrchestrator) {
        this.scanOrchestrator = scanOrchestrator;
    }

    public String submit(String repoUrl) {
        String id = UUID.randomUUID().toString();
        statusById.put(id, ScanJobStatus.RUNNING);
        runAsync(id, repoUrl);
        return id;
    }

    @Async("scanExecutor")
    protected CompletableFuture<Void> runAsync(String id, String repoUrl) {
        try {
            ScanResult result = scanOrchestrator.runScan(id, repoUrl);
            resultById.put(id, result);
            statusById.put(id, ScanJobStatus.SUCCEEDED);
        } catch (Exception ex) {
            errorById.put(id, ex.getMessage());
            statusById.put(id, ScanJobStatus.FAILED);
        }
        return CompletableFuture.completedFuture(null);
    }

    public ScanJobStatus getStatus(String id) {
        return statusById.get(id);
    }

    public ScanResult getResult(String id) {
        return resultById.get(id);
    }

    public String getError(String id) {
        return errorById.get(id);
    }
}
