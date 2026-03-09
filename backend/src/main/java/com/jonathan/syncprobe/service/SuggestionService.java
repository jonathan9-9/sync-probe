package com.jonathan.syncprobe.service;

import com.jonathan.syncprobe.model.Chunk;
import com.jonathan.syncprobe.model.Suggestion;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

// Google AI Agent
@Service
public class SuggestionService {
    private final ChatModel chatModel;

    public SuggestionService(ObjectProvider<ChatModel> chatModelProvider) {
        this.chatModel = chatModelProvider.getIfAvailable();
    }

    public List<Suggestion> generateFixes(List<Chunk> staleDocs, List<Chunk> relatedCodeChunks) {
        List<Suggestion> suggestions = new ArrayList<>();
        if (staleDocs == null || staleDocs.isEmpty()) {
            suggestions.add(new Suggestion("No stale documentation chunks detected."));
            return suggestions;
        }

        if (chatModel != null) {
            try {
                String aiResponse = chatModel.chat(
                        SystemMessage.from("You are a senior engineer. Suggest concise doc fixes based on stale doc chunks and related code. Return one actionable bullet per line."),
                        UserMessage.from(buildPrompt(staleDocs, relatedCodeChunks))
                ).aiMessage().text();

                List<Suggestion> parsed = parseSuggestions(aiResponse);
                if (!parsed.isEmpty()) {
                    return parsed;
                }
            } catch (RuntimeException ignored) {
                // Fallback to deterministic suggestions if AI call fails.
            }
        }

        for (Chunk staleDoc : staleDocs) {
            if (staleDoc == null) continue;
            String path = staleDoc.getPath() == null || staleDoc.getPath().isBlank() ? "unknown-file" : staleDoc.getPath();
            String chunkId = staleDoc.getId() == null || staleDoc.getId().isBlank() ? "unknown-chunk" : staleDoc.getId();
            suggestions.add(new Suggestion(
                    "Low-similarity doc chunk detected in " + path + " (" + chunkId + "). Review and update this section."
            ));
        }
        return suggestions;
    }

    private String buildPrompt(List<Chunk> staleDocs, List<Chunk> relatedCodeChunks) {
        String staleSection = staleDocs.stream()
                .filter(Objects::nonNull)
                .map(chunk -> formatChunk("DOC", chunk))
                .collect(Collectors.joining("\n\n"));

        String codeSection = relatedCodeChunks.stream()
                .filter(chunk -> chunk != null && "code".equalsIgnoreCase(chunk.getType()))
                .map(chunk -> formatChunk("CODE", chunk))
                .collect(Collectors.joining("\n\n"));

        if (codeSection.isBlank()) {
            codeSection = "No related code chunks were found.";
        }

        return "Stale documentation chunks:\n" + staleSection + "\n\nRelated code chunks:\n" + codeSection;
    }

    private String formatChunk(String label, Chunk chunk) {
        String path = chunk.getPath() == null ? "unknown-path" : chunk.getPath();
        String id = chunk.getId() == null ? "unknown-id" : chunk.getId();
        String symbol = chunk.getSymbol() == null ? "" : (" symbol=" + chunk.getSymbol());
        String content = chunk.getContent() == null ? "" : chunk.getContent();
        String clipped = content.length() > 900 ? content.substring(0, 900) + "..." : content;
        return "[" + label + "] path=" + path + " id=" + id + symbol + "\n" + clipped;
    }

    private List<Suggestion> parseSuggestions(String aiResponse) {
        List<Suggestion> suggestions = new ArrayList<>();
        if (aiResponse == null || aiResponse.isBlank()) {
            return suggestions;
        }
        for (String line : aiResponse.split("\\R")) {
            String cleaned = line.replaceFirst("^\\s*[-*\\d.)]+\\s*", "").trim();
            if (!cleaned.isEmpty()) {
                suggestions.add(new Suggestion(cleaned));
            }
        }
        return suggestions;
    }
}
