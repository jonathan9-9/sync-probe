package com.jonathan.syncprobe.controller.dto;

import jakarta.validation.constraints.NotBlank;

public class ScanRequestDto {
    @NotBlank(message = "repoUrl is required")
    public String repoUrl;

    public ScanRequestDto() {}

    public ScanRequestDto(String repoUrl) {
        this.repoUrl = repoUrl;
    }
}
