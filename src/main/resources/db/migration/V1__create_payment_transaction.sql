CREATE TABLE payment_transaction (
    id UUID PRIMARY KEY,
    message_id VARCHAR(35) NOT NULL,
    creation_date_time TIMESTAMP WITH TIME ZONE NOT NULL,
    number_of_transactions INTEGER NOT NULL CHECK (number_of_transactions > 0),
    settlement_method VARCHAR(16) NOT NULL,
    instruction_id VARCHAR(35) NOT NULL,
    end_to_end_id VARCHAR(35) NOT NULL,
    transaction_id VARCHAR(35) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    settlement_date DATE NOT NULL,
    debtor_name VARCHAR(140) NOT NULL,
    debtor_iban VARCHAR(34) NOT NULL,
    debtor_bic VARCHAR(11) NOT NULL,
    creditor_name VARCHAR(140) NOT NULL,
    creditor_iban VARCHAR(34) NOT NULL,
    creditor_bic VARCHAR(11) NOT NULL,
    remittance_information VARCHAR(140) NOT NULL,
    payment_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_payment_transaction_transaction_id UNIQUE (transaction_id)
);

CREATE INDEX idx_payment_transaction_message_id ON payment_transaction (message_id);
CREATE INDEX idx_payment_transaction_created_at ON payment_transaction (created_at);
