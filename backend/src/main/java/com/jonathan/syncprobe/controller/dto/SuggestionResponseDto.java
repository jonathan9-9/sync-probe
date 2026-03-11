package com.jonathan.syncprobe.controller.dto;

import com.jonathan.syncprobe.model.Suggestion;

import java.util.List;

public class SuggestionResponseDto {
    public List<Suggestion> suggestions;

    public SuggestionResponseDto() {}

    public SuggestionResponseDto(List<Suggestion> suggestions) {
        this.suggestions = suggestions;
    }
}
