package com.jonathan.syncprobe.model;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmbeddingChunk {

    private Chunk chunk;
    private double[] embedding;

    public EmbeddingChunk(Chunk chunk, double[] embedding) {
        this.chunk = chunk;
        this.embedding = embedding;
    }
}