CREATE INDEX idx_payment_debtor_creation
    ON payment_transaction (debtor_iban, creation_date_time DESC);

CREATE INDEX idx_payment_debtor_creditor_creation
    ON payment_transaction (debtor_iban, creditor_iban, creation_date_time DESC);

CREATE INDEX idx_payment_debtor_status_creation
    ON payment_transaction (debtor_iban, payment_status, creation_date_time DESC);
