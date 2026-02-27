package com.jonathan.syncprobe.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class ChromaConfig {
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(){
        return ChromaEmbeddingStore.builder()
                .baseUrl("http://localhost:8000")
                .collectionName("doc-code-vectors")
                .build();
    }
}