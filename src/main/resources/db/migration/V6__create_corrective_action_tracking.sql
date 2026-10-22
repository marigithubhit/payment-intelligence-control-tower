CREATE TABLE corrective_action (
    id UUID PRIMARY KEY,
    case_id UUID REFERENCES investigation_case (id),
    transaction_id VARCHAR(35) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    assigned_team VARCHAR(64) NOT NULL,
    assigned_role VARCHAR(64) NOT NULL,
    assigned_to VARCHAR(100),
    human_approval_required BOOLEAN NOT NULL DEFAULT TRUE,
    approval_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    approved_by VARCHAR(100),
    approved_at TIMESTAMP WITH TIME ZONE,
    approval_notes VARCHAR(2000),
    target_metric VARCHAR(100),
    baseline_metric_value VARCHAR(64),
    post_action_metric_value VARCHAR(64),
    outcome_status VARCHAR(32) NOT NULL DEFAULT 'PENDING_EVALUATION',
    outcome_notes VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_corrective_action_case_id ON corrective_action (case_id);
CREATE INDEX idx_corrective_action_txn_id ON corrective_action (transaction_id);
CREATE INDEX idx_corrective_action_status ON corrective_action (status);
CREATE INDEX idx_corrective_action_team ON corrective_action (assigned_team);

CREATE TABLE corrective_action_note (
    id UUID PRIMARY KEY,
    action_id UUID NOT NULL,
    author VARCHAR(100) NOT NULL,
    role VARCHAR(64) NOT NULL,
    note VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_action_note_action FOREIGN KEY (action_id)
        REFERENCES corrective_action (id) ON DELETE CASCADE
);

CREATE INDEX idx_corrective_action_note_action_date ON corrective_action_note (action_id, created_at);
