package com.jonathan.syncprobe.persistence.entity;

import com.jonathan.syncprobe.model.ScanJobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Entity
@Table(name = "scans")
public class ScanRecord {

    @Setter
    @Id
    private String id;

    @Setter
    @Column(name = "repo_url")
    private String repoUrl;

    @Setter
    @Enumerated(EnumType.STRING)
    private ScanJobStatus status;

    @Setter
    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson;

    @Setter
    @Column(name = "error", columnDefinition = "TEXT")
    private String error;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Setter
    @Column(name = "finished_at")
    private Instant finishedAt;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // getters and setters

}

