//package com.jonathan.syncprobe.config;
//
//import dev.langchain4j.model.embedding.EmbeddingModel;
//import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
///**
// * Provides a default embedding model so the application can start
// * even if a remote provider isn't configured.
// */
//@Configuration
//public class EmbeddingConfig {
//
//    @Bean
//    public EmbeddingModel embeddingModel() {
//        return new AllMiniLmL6V2EmbeddingModel();
//    }
//}
