package com.jonathan.syncprobe.persistence.repository;

import com.jonathan.syncprobe.persistence.entity.ChunkEmbeddingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChunkEmbeddingRepository extends JpaRepository<ChunkEmbeddingRecord, Long> {
    List<ChunkEmbeddingRecord> findByScanId(String scanId);
}

