# Application Architecture

The application is a Java 21 / Spring Boot modular monolith. Controllers handle HTTP binding, services coordinate use cases, validation rules remain deterministic and independently testable, repositories isolate database access, and Flyway versions the PostgreSQL schema.

## Payment ingestion and investigation

```text
ISO 20022 Payment
       ↓
Payment Transaction
       ↓
Deterministic Validation
       ↓
Historical Analysis
       ↓
Evidence Collection
       ↓
Phase 5 Persistence (case and evidence)
       ↓
Evidence Agent
       ↓
Risk Agent
       ↓
Ollama Reasoning Agent (advisory; optional)
       ↓
Investigation Result with all agent assessments
       ↓
PostgreSQL
```

## Persistence model

```text
Payment Transaction (business key: transaction_id)
        ↓ (referenced by business ID in case data)
Many InvestigationCase rows (UUID case IDs; repeated runs retained)
        ↓ (foreign key investigation_case_id)
Many InvestigationEvidence rows
```

An investigation case records its result and timestamps without copying payment details. Evidence is attached to exactly one case. The database enforces case/evidence referential integrity and supports many cases per payment so later re-investigations do not overwrite the audit trail.

Flyway V1 creates the payment table, V2 creates the case and evidence tables, and V3 adds indexes for historical queries. Hibernate validates the schema at startup. The `test` profile uses H2 only for automated tests and applies the same migrations; runtime configuration remains PostgreSQL.

## Investigation execution and history

`GET /api/v1/investigations/{transactionId}` loads the payment, runs Phase 4 rules, analyzes the configured history window, and persists the Phase 5 case plus Phase 4/6 evidence. The Evidence Agent then assesses the persisted case evidence, and the Risk Agent receives that assessment plus the deterministic findings and history. Their result is appended to the API response; it does not replace Phase 4 status, risk level, or recommendation and is not persisted. Calling the endpoint again creates another case. `GET /api/v1/investigations/{transactionId}/history` returns historical analysis without creating a case. `GET /api/v1/investigations/cases/{caseId}` loads persisted case evidence through DTOs.

Validation finding evidence identifies `Phase4ValidationEngine` as its source. Historical evidence identifies `PaymentTransactionRepository` and includes observed facts or derived aggregates in existing evidence columns. The window is inclusive from current creation time minus `historical-analysis.window-days` through current creation time, with current row exclusion. Amount aggregates use successful same-beneficiary records and matching currency. No historical claim is emitted when supporting rows are absent. Flyway V3 adds indexes aligned with debtor/time, debtor/creditor/time and debtor/status/time queries.

# Agent orchestration

The investigation API preserves its deterministic validation, historical analysis, evidence collection, and risk assessment. Agent orchestration then runs Evidence -> Risk -> Reasoning. Phase 8 adds an advisory Ollama reasoning result to `agentAssessment`; it does not replace deterministic fields, persist AI output, or add a database migration. Ollama failure is contained and represented by a reasoning status.

The reasoning response includes mapped evidence references, separate reasoning confidence, configured model and prompt version, and execution time. Outputs with malformed JSON, extra fields, transaction mismatch, unsupported evidence references, unsupported uncertainties, or a weaker analyst action are rejected. Reasoning confidence is capped at 0.50 when the Evidence Agent marks the evidence insufficient.
