CREATE TABLE investigation_case (
    id UUID PRIMARY KEY,
    transaction_id VARCHAR(35) NOT NULL,
    status VARCHAR(32) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    recommendation VARCHAR(64) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    opened_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_investigation_case_transaction_id ON investigation_case (transaction_id);
CREATE INDEX idx_investigation_case_created_at ON investigation_case (created_at);

CREATE TABLE investigation_evidence (
    id UUID PRIMARY KEY,
    investigation_case_id UUID NOT NULL,
    evidence_type VARCHAR(64) NOT NULL,
    category VARCHAR(64),
    rule_code VARCHAR(64),
    severity VARCHAR(32),
    description VARCHAR(1000) NOT NULL,
    source_reference VARCHAR(255) NOT NULL,
    observed_value VARCHAR(500),
    expected_value VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_investigation_evidence_case FOREIGN KEY (investigation_case_id)
        REFERENCES investigation_case (id) ON DELETE CASCADE
);

CREATE INDEX idx_investigation_evidence_case_id ON investigation_evidence (investigation_case_id);
