package com.jonathan.syncprobe.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Entity
@Table(name = "chunk_embeddings")
public class ChunkEmbeddingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(name = "scan_id")
    private String scanId;

    @Setter
    @Column(name = "chunk_id")
    private String chunkId;

    @Setter
    @Column(name = "chunk_type")
    private String chunkType;

    @Setter
    private String path;

    @Setter
    private String symbol;

    @Setter
    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    @Setter
    @Column(name = "embedding_id")
    private String embeddingId;

    @Setter
    @Lob
    @Column(name = "vector", columnDefinition = "TEXT")
    private String vector;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }

}

