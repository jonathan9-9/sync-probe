package com.jonathan.syncprobe.persistence.repository;

import com.jonathan.syncprobe.persistence.entity.ScanRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanRecordRepository extends JpaRepository<ScanRecord, String> {
}

