CREATE TABLE security_findings (
    id BIGSERIAL PRIMARY KEY,
    finding_id VARCHAR(150) NOT NULL UNIQUE,
    vulnerability_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    identity VARCHAR(100) NOT NULL,
    endpoint VARCHAR(500) NOT NULL,
    target_resource VARCHAR(150) NOT NULL,
    description TEXT,
    expected_status INTEGER NOT NULL,
    actual_status INTEGER NOT NULL,
    evidence TEXT,
    detected_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_findings_severity
    ON security_findings(severity);

CREATE INDEX idx_findings_type
    ON security_findings(vulnerability_type);

CREATE INDEX idx_findings_detected_at
    ON security_findings(detected_at);

    