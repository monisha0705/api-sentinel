package com.apisentinel.sentinel.finding;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "security_findings")
public class SecurityFindingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "finding_id", nullable = false, unique = true, length = 150)
    private String findingId;

    @Column(name = "vulnerability_type", nullable = false, length = 50)
    private String vulnerabilityType;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column(nullable = false, length = 100)
    private String identity;

    @Column(nullable = false, length = 500)
    private String endpoint;

    @Column(name = "target_resource", nullable = false, length = 150)
    private String targetResource;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "expected_status", nullable = false)
    private int expectedStatus;

    @Column(name = "actual_status", nullable = false)
    private int actualStatus;

    @Column(columnDefinition = "TEXT")
    private String evidence;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    protected SecurityFindingEntity() {
    }

    public SecurityFindingEntity(SecurityFinding finding) {
        this.findingId = finding.findingId();
        this.vulnerabilityType = finding.vulnerabilityType();
        this.severity = finding.severity();
        this.identity = finding.identity();
        this.endpoint = finding.endpoint();
        this.targetResource = finding.targetResource();
        this.description = finding.description();
        this.expectedStatus = finding.expectedStatus();
        this.actualStatus = finding.actualStatus();
        this.evidence = finding.evidence();
        this.detectedAt = finding.detectedAt();
    }

    public Long getId() { return id; }
    public String getFindingId() { return findingId; }
    public String getVulnerabilityType() { return vulnerabilityType; }
    public String getSeverity() { return severity; }
    public String getIdentity() { return identity; }
    public String getEndpoint() { return endpoint; }
    public String getTargetResource() { return targetResource; }
    public String getDescription() { return description; }
    public int getExpectedStatus() { return expectedStatus; }
    public int getActualStatus() { return actualStatus; }
    public String getEvidence() { return evidence; }
    public Instant getDetectedAt() { return detectedAt; }
}

