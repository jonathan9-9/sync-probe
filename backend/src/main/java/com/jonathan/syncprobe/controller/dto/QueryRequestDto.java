package com.jonathan.syncprobe.controller.dto;

import jakarta.validation.constraints.NotBlank;

public class QueryRequestDto {
    @NotBlank(message = "query is required")
    public String query;

    public QueryRequestDto() {}

    public QueryRequestDto(String query) {
        this.query = query;
    }
}
