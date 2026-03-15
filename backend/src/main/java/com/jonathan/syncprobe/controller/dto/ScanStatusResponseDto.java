package com.jonathan.syncprobe.controller.dto;

import com.jonathan.syncprobe.model.ScanJobStatus;
import com.jonathan.syncprobe.model.ScanResult;

public class ScanStatusResponseDto {
    public ScanJobStatus status;
    public ScanResult result;
    public String error;

    public ScanStatusResponseDto() {}

    public ScanStatusResponseDto(ScanJobStatus status, ScanResult result, String error) {
        this.status = status;
        this.result = result;
        this.error = error;
    }
}
