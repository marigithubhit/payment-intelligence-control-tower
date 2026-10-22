# Deterministic Investigation Engine

## Flow

1. The pacs.008 parser converts XML to the typed `Iso20022Payment` DTO.
2. During ingestion, the validation service evaluates deterministic rules before persistence. A duplicate finding is translated to the existing HTTP 409 `DUPLICATE_TRANSACTION` response; other findings are returned with the successful ingestion result.
3. `GET /api/v1/investigations/{transactionId}` loads the persisted row and queries the debtor's prior transactions.
4. The independently testable rules produce structured `ValidationFinding` records. The validation result also carries historical payment evidence sourced from the repository.
5. `RiskAssessmentService` applies fixed severity thresholds, derives the investigation status and selects a recommendation.
6. Each completed run is saved as a new `InvestigationCase`; failed rule findings and related historical payment evidence become child `InvestigationEvidence` rows.
7. The case ID is returned by the investigation API and can retrieve the persisted case and evidence.

Controllers only bind requests and responses. `InvestigationService` orchestrates repository access, rules, assessment and case persistence. `InvestigationPersistenceService` maps a result to the case/evidence aggregate transactionally. Individual `ValidationRule` components contain single deterministic checks. They do not call AI services or infer unsupported facts.

## Finding fields

Each failed rule returns `ruleCode`, `severity`, `category`, `message`, `field`, `evidence` and `passed=false`. Passing checks do not create findings. Finding order is sorted by rule code so equivalent evidence yields stable JSON.

Every failed finding is persisted as `VALIDATION_FINDING` evidence from `Phase4ValidationEngine`. Its description includes the actual rule evidence text. Historical transactions considered for the same debtor/beneficiary are stored as `HISTORICAL_PAYMENT` evidence from `PaymentTransactionRepository`. The case stores null observed/expected fields unless an actual structured value is supplied; current rule results do not expose those separately.

Every explicit investigation request creates a fresh case. Existing cases are never overwritten. The case's child evidence rows use a foreign key and are retrieved by `GET /api/v1/investigations/cases/{caseId}`.

## Rules and data boundaries

| Rule | Data and behavior | Severity |
|---|---|---|
| `MISSING_REQUIRED_FIELD` | Requires business/message IDs, amount, currency, party names and both IBANs. | HIGH |
| `INVALID_AMOUNT` | Amount must be a positive `BigDecimal`. | HIGH |
| `UNSUPPORTED_CURRENCY` | Reads the configurable comma-separated currency set; defaults to AED, USD, EUR and GBP. | MEDIUM |
| `INVALID_ACCOUNT_FORMAT` | Basic shape only: country letters, check digits, then 11–30 alphanumeric characters. No account-existence or checksum claim. | HIGH |
| `DUPLICATE_TRANSACTION` | Checks the repository by business transaction ID. During an investigation, excludes the current database row. The database unique constraint remains authoritative for concurrent inserts. | HIGH |
| `BENEFICIARY_MISMATCH` | Compares same debtor IBAN and normalized creditor name against prior `SUCCESS` records. It flags only if every qualifying successful account differs from the current account and includes transaction IDs/accounts in evidence. | HIGH |
| `REPEATED_PAYMENT_FAILURE` | Same debtor IBAN and normalized beneficiary with `FAILED` outcomes created in the configured 90-day window. Three to five is MEDIUM; six or more is HIGH. | MEDIUM / HIGH |
| `AMOUNT_ANOMALY` | Requires at least three prior successful payments with the same debtor, normalized creditor and currency. Compares current amount to their arithmetic mean: above 3x is MEDIUM, above 6x is HIGH. | MEDIUM / HIGH |

Beneficiary normalization lowercases and removes punctuation and spaces. Historical account checks normalize spaces and letter case. The anomaly rule uses successful history only and keeps currencies separate. It is an unusual-amount indicator, never a fraud finding.

## Risk and recommendation

- CRITICAL: any CRITICAL finding or at least two HIGH findings.
- HIGH: one HIGH finding or at least two MEDIUM findings.
- MEDIUM: one MEDIUM finding.
- LOW: no MEDIUM-or-higher finding.

Any MEDIUM, HIGH or CRITICAL finding makes status `REQUIRES_REVIEW`; otherwise it is `CLEAR`. `CLEAR` maps to `NO_ACTION`. A LOW result maps to `MONITOR` if there is a low finding. MEDIUM maps to `REVIEW`, HIGH to `MANUAL_REVIEW`, and CRITICAL to `HOLD_AND_ESCALATE`.

These thresholds are intentionally small and explainable for a demonstration. They are not a production risk policy.

## Evidence and future AI boundary

Historical evidence is built only from database rows with the same debtor IBAN and normalized creditor name. A beneficiary mismatch cites the exact successful transaction IDs and creditor IBANs compared. Failure and amount findings cite the historical transaction IDs and counts/amount averages used. Empty history produces no invented mismatch or anomaly baseline.

The response is structured for a later reasoning component, but this phase has no LLM, agent, RAG, or external decision provider. A future AI should receive this result as prepared evidence and should not query the database itself.

## Synthetic test scenarios

The test suite uses H2 with the same Flyway migration and synthetic records created by test fixtures only:

1. No adverse history: CLEAR / LOW / NO_ACTION.
2. Prior successful payment to another account for the same named beneficiary: BENEFICIARY_MISMATCH with the historical transaction ID.
3. Three recent failed payments: REPEATED_PAYMENT_FAILURE.
4. Three historical successful amounts of AED 1,000 and current amount AED 4,000: AMOUNT_ANOMALY.
5. Malformed IBAN text: INVALID_ACCOUNT_FORMAT; no account existence claim.
6. Three historical successful payments to another account plus a current AED 10,000 payment against a AED 1,000 average: two HIGH findings, CRITICAL risk, HOLD_AND_ESCALATE.

These fixtures do not seed or modify the regular PostgreSQL database.
