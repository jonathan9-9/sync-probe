package com.jonathan.syncprobe.vector;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.stereotype.Service;

@Service
public class VectorStoreService {
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    public VectorStoreService(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel){
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }
    public void store(String content){
        TextSegment segment = TextSegment.from(content);
        var embedding = embeddingModel.embed(segment).content();
        embeddingStore.add(embedding, segment);
    }
}
