package com.jonathan.syncprobe.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChromaConfig {

    @Value("${chroma.base-url:}")
    private String chromaBaseUrl;

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(){
        if (chromaBaseUrl == null || chromaBaseUrl.isBlank()) {
            throw new IllegalStateException("chroma.base-url is not set; set it in application.yaml or env to enable Chroma persistence.");
        }

        return ChromaEmbeddingStore.builder()
                .baseUrl(chromaBaseUrl)
                .collectionName("doc-code-vectors")
                .build();
    }
}
