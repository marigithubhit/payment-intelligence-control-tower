ALTER TABLE investigation_case ADD COLUMN human_review_required BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE investigation_review (
    id UUID PRIMARY KEY,
    investigation_case_id UUID NOT NULL,
    decision VARCHAR(16) NOT NULL,
    reason VARCHAR(2000) NOT NULL,
    reviewer VARCHAR(100) NOT NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    previous_status VARCHAR(32) NOT NULL,
    new_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_investigation_review_case FOREIGN KEY (investigation_case_id)
        REFERENCES investigation_case (id)
);

CREATE INDEX idx_investigation_review_case_reviewed
    ON investigation_review (investigation_case_id, reviewed_at, created_at);
