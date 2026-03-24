package com.jonathan.syncprobe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathan.syncprobe.model.ScanJobStatus;
import com.jonathan.syncprobe.model.ScanResult;
import com.jonathan.syncprobe.orchestrator.ScanOrchestrator;
import com.jonathan.syncprobe.persistence.entity.ScanRecord;
import com.jonathan.syncprobe.persistence.repository.ScanRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScanJobServiceTest {

    @Mock
    private ScanOrchestrator scanOrchestrator;

    @Mock
    private ScanRecordRepository scanRecordRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ScanJobService scanJobService;

    private Map<String, ScanRecord> db;

    @BeforeEach
    void setup() {
        db = new ConcurrentHashMap<>();
        when(scanRecordRepository.save(any())).thenAnswer(invocation -> {
            ScanRecord record = invocation.getArgument(0);
            db.put(record.getId(), record);
            return record;
        });
        when(scanRecordRepository.findById(any())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            return Optional.ofNullable(db.get(id));
        });

        scanJobService = new ScanJobService(scanOrchestrator, scanRecordRepository, objectMapper);
    }

    @Test
    void submitPersistsRunningRecord() {
        when(scanOrchestrator.runScan(anyString(), anyString()))
                .thenReturn(new ScanResult(null));

        String id = scanJobService.submit("https://example.com/repo.git");

        assertThat(db.get(id)).isNotNull();
        assertThat(db.get(id).getStatus()).isIn(ScanJobStatus.RUNNING, ScanJobStatus.SUCCEEDED);
    }

    @Test
    void runAsyncMarksSuccessAndStoresResult() {
        ScanResult result = new ScanResult(null);
        when(scanOrchestrator.runScan(anyString(), anyString())).thenReturn(result);

        String id = UUID.randomUUID().toString();
        ScanRecord record = new ScanRecord();
        record.setId(id);
        record.setRepoUrl("https://example.com/repo.git");
        record.setStatus(ScanJobStatus.RUNNING);
        db.put(id, record);

        scanJobService.runAsync(id, record.getRepoUrl());

        assertThat(scanJobService.getStatus(id)).isEqualTo(ScanJobStatus.SUCCEEDED);
        assertThat(scanJobService.getResult(id)).isNotNull();
    }

    @Test
    void runAsyncMarksFailureWhenExceptionThrown() {
        when(scanOrchestrator.runScan(anyString(), anyString()))
                .thenThrow(new RuntimeException("boom"));

        String id = UUID.randomUUID().toString();
        ScanRecord record = new ScanRecord();
        record.setId(id);
        record.setRepoUrl("https://example.com/repo.git");
        record.setStatus(ScanJobStatus.RUNNING);
        db.put(id, record);

        scanJobService.runAsync(id, record.getRepoUrl());

        assertThat(scanJobService.getStatus(id)).isEqualTo(ScanJobStatus.FAILED);
        assertThat(scanJobService.getError(id)).contains("boom");
    }
}
