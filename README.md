# ISO 20022 AI Investigation Assistant

**Agentic AI for Intelligent Payment Investigation, Evidence, Risk Analysis and Verification**

A staged banking/FinTech demonstration of evidence-based payment investigation. All payment data in this repository is synthetic demo data. AI recommendations in the eventual system will be advisory, high-risk cases will require human review, and the system is not connected to real banking systems. This project does not claim regulatory certification or production readiness.

## Technology

Java 21, Spring Boot 3.5.16, Maven, Spring Web, Spring Data JPA, PostgreSQL and Flyway migrations.

## Phase 1 - Foundation

```http
GET /api/v1/health
```

## Phase 2 - ISO 20022 pacs.008

`pacs.008` is the ISO 20022 Financial Institution to Financial Institution Customer Credit Transfer message. The parser accepts the `pacs.008.001.08` namespace and extracts the message header, transaction identifiers, settlement information, amount and currency, debtor/creditor names, IBANs and BICs, and remittance text into a typed `Iso20022Payment` record.

The namespace-aware XML parser disables DTDs, external entities, external schema access and XInclude. The synthetic sample is at [`sample-pacs008-payment.xml`](src/main/resources/iso20022/pacs008/sample-pacs008-payment.xml).

## Phase 3 - PostgreSQL Payment Persistence

Successfully parsed payments are mapped to `PaymentTransaction` and stored in PostgreSQL. The relational table provides the durable transaction history needed for later investigation, historical comparisons and an auditable payment trail.

The primary key `id` is an internal randomly generated UUID used to identify the database row. The ISO 20022 `transactionId` remains the business identifier and has a database uniqueness constraint; it is not used as the row's primary key. New rows have status `RECEIVED`. Amounts use `NUMERIC(19,4)` / Java `BigDecimal`, and dates/timestamps use typed temporal columns.

Flyway migration `V1__create_payment_transaction.sql` creates `payment_transaction`, its transaction ID uniqueness constraint, and indexes for message ID and creation time. Hibernate uses `ddl-auto=validate`; the application does not create or alter the production schema.

### Local PostgreSQL setup

Create the local role and database in `psql` (replace the password placeholder with a local secret):

```sql
CREATE ROLE iso20022_app LOGIN PASSWORD 'replace-with-a-local-secret';
CREATE DATABASE iso20022_ai OWNER iso20022_app;
```

Configure the connection through environment variables; no database password is committed in this repository.

PowerShell example for the current shell:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/iso20022_ai'
$env:DB_USERNAME = 'your_local_database_user'
$env:DB_PASSWORD = 'your_local_database_password'
mvn spring-boot:run
```

`DB_URL` defaults to `jdbc:postgresql://localhost:5432/iso20022_ai`; set `DB_USERNAME` and `DB_PASSWORD` for the local database. At startup Flyway applies pending migrations and Hibernate validates the mapped schema. The PostgreSQL JDBC driver is included at runtime.

### Ingestion and duplicates

`POST /api/v1/payments/iso20022` accepts pacs.008 XML (`Content-Type: application/xml`). The service parses the message, checks whether its business transaction ID has already been received, maps the typed DTO to an entity, and persists it before returning success. A duplicate is returned as HTTP 409 with code `DUPLICATE_TRANSACTION`. The pre-check gives a clear response for ordinary retries; the unique database constraint also prevents duplicate rows during concurrent requests.

Example request:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/payments/iso20022 `
  -ContentType 'application/xml' `
  -InFile src/main/resources/iso20022/pacs008/sample-pacs008-payment.xml
```

Example success response:

```json
{
  "status": "RECEIVED",
  "messageId": "MARI202610060001",
  "transactionId": "TXN100001",
  "amount": 25000.00,
  "currency": "AED",
  "debtor": "ABC Trading LLC",
  "creditor": "XYZ Supplies LLC",
  "debtorIban": "AE070331234567890123456",
  "creditorIban": "AE120331987654321098765",
  "persisted": true,
  "findings": []
}
```

Verify a stored row with `psql`:

```sql
SELECT id, transaction_id, amount, currency, payment_status, created_at
FROM payment_transaction
WHERE transaction_id = 'TXN100001';
```

## Run and test

Requirements: JDK 21, Maven 3.6.3 or later, and a reachable PostgreSQL database for normal application startup.

```powershell
mvn test
mvn package
mvn spring-boot:run
```

Automated persistence and API tests use an isolated in-memory H2 database in PostgreSQL compatibility mode and run the same Flyway migration. PostgreSQL itself remains the configured runtime database; the H2 profile is test-only and does not change production configuration.

## Phase 4 — Payment Validation & Investigation

Deterministic validation runs before any future AI. Payment facts such as amount, currency, account format, duplicates and historical patterns are checked with explicit rules, and every failed rule returns a structured finding. No LLM is used.

### Rules and historical evidence

- Required payment fields: `transactionId`, `messageId`, amount, currency, debtor/creditor names, and debtor/creditor IBANs. Missing data is HIGH severity.
- Amount must be a positive `BigDecimal`; otherwise `INVALID_AMOUNT` is HIGH.
- Supported currencies default to AED, USD, EUR and GBP (`investigation.rules.supported-currencies`). An unconfigured currency is MEDIUM.
- A basic IBAN shape check requires a two-letter country prefix, two check digits and 11–30 alphanumeric characters. This is a format check only; it does not prove an account exists.
- Existing business transaction IDs are reported as `DUPLICATE_TRANSACTION` and continue to return HTTP 409 on ingestion.
- Beneficiary consistency compares the current debtor IBAN and normalized creditor name with actual prior successful payments. A HIGH mismatch is returned only when qualifying history exists and none of those successful payments used the current creditor IBAN; evidence includes the historical transaction IDs and accounts. With no qualifying history or any matching account, no mismatch is inferred.
- Repeated failures are counted for the same debtor IBAN and normalized creditor name over the prior 90 days. Three to five failures are MEDIUM; six or more are HIGH (`investigation.rules.repeated-failure-threshold`, `investigation.rules.recent-failure-window-days`).
- Amount anomaly analysis requires at least three prior successful payments with the same debtor, normalized creditor name and currency. The current amount must exceed 3 times their average (`investigation.rules.amount-anomaly-multiplier`) to be flagged. Above 3x through 6x is MEDIUM; above 6x is HIGH. This is an anomaly indicator, not a fraud conclusion.

History evidence is read from persisted transactions and scoped to the same debtor and normalized beneficiary. No demo records are automatically inserted into a normal database.

### Risk, status and recommendation

Risk scoring is transparent: any CRITICAL finding or two or more HIGH findings produces CRITICAL; one HIGH or at least two MEDIUM findings produces HIGH; one MEDIUM produces MEDIUM; otherwise risk is LOW. Any MEDIUM-or-higher finding sets `REQUIRES_REVIEW`; no such findings produces `CLEAR`. Recommendations map CLEAR to `NO_ACTION`, MEDIUM to `REVIEW`, HIGH to `MANUAL_REVIEW`, and CRITICAL to `HOLD_AND_ESCALATE`. LOW with a finding maps to `MONITOR`.

### Investigation API

```http
GET /api/v1/investigations/{transactionId}
```

The API loads the persisted payment, gathers historical records, executes the rules and returns structured findings plus the history used. Unknown transaction IDs return HTTP 404 with code `TRANSACTION_NOT_FOUND`.

Example mismatch response (evidence values are synthetic):

```json
{
  "transactionId": "TXN100004",
  "status": "REQUIRES_REVIEW",
  "riskLevel": "HIGH",
  "findings": [
    {
      "ruleCode": "BENEFICIARY_MISMATCH",
      "severity": "HIGH",
      "category": "BENEFICIARY",
      "message": "Creditor account differs from historical successful payments",
      "field": "creditorIban",
      "evidence": "Current creditor IBAN differs from Historical successful payment evidence: TXN099991 used AE980331111122223333444",
      "passed": false
    }
  ],
  "historicalEvidence": [
    {
      "transactionId": "TXN099991",
      "paymentStatus": "SUCCESS",
      "amount": 4000.0000,
      "currency": "AED",
      "creditorName": "XYZ Supplies LLC",
      "creditorIban": "AE980331111122223333444"
    }
  ],
  "recommendation": "MANUAL_REVIEW"
}
```

### Repeatable scenarios

The test suite uses synthetic fixtures in an isolated H2 database and runs Flyway migrations V1 through V3. It covers a clean payment, an evidence-backed beneficiary mismatch, repeated failures, amount anomaly, invalid account format, and multiple findings that escalate to CRITICAL. It also verifies that missing history never invents a mismatch. Run these without touching the normal PostgreSQL data:

```powershell
mvn -Dtest=PaymentValidationServiceTest,InvestigationControllerTest,RiskAssessmentServiceTest test
```

See [`docs/investigation-engine.md`](docs/investigation-engine.md) for the detailed rule flow and evidence boundaries.

## Phase 5 — Investigation Case & Evidence Persistence

Every call to `GET /api/v1/investigations/{transactionId}` now creates a new investigation case after deterministic validation and risk assessment. A case stores its UUID, business transaction ID, status, risk level, recommendation, deterministic summary and lifecycle timestamps. It does not duplicate payment fields; `payment_transaction` remains the source of payment data. Re-investigating a payment creates another case and preserves earlier results.

Each failed validation finding is stored as `VALIDATION_FINDING` evidence from `Phase4ValidationEngine`, including its rule, severity, message and rule evidence text. Phase 6 enriches that case with typed historical evidence derived from persisted payment rows. Evidence is traceable to `PaymentTransactionRepository`; amounts, beneficiary accounts and outcome counts are stored in existing observed/expected fields without changing the Phase 5 schema.

Flyway migration `V2__create_investigation_case_and_evidence.sql` creates `investigation_case` and `investigation_evidence`. Each case can have many evidence rows, with a foreign key and cascade delete. Transaction ID is indexed but not unique so case history can contain multiple runs. Migration V3 adds indexes for debtor/date, debtor/creditor/date, and debtor/status/date history queries.

The existing investigation response retains its Phase 4 fields and adds `caseId`, `summary`, `openedAt`, and `completedAt`. Retrieve a stored case and its evidence with:

```http
GET /api/v1/investigations/cases/{caseId}
```

Summaries are deterministic: clear cases state that all configured rules passed; cases with multiple findings state that review is required due to multiple findings; a single high-severity finding states that manual review is required. Phase 7 adds deterministic Evidence and Risk Agents after Phase 6 completes the evidence layer; LLM reasoning remains deferred.

Verify the audit trail in PostgreSQL:

```sql
SELECT * FROM public.investigation_case ORDER BY created_at DESC;
SELECT * FROM public.investigation_evidence ORDER BY created_at DESC;

SELECT ic.id AS case_id, ic.transaction_id, ic.status, ic.risk_level,
       ic.recommendation, ie.rule_code, ie.severity, ie.description
FROM public.investigation_case ic
LEFT JOIN public.investigation_evidence ie ON ie.investigation_case_id = ic.id
ORDER BY ic.created_at DESC;
```

## Phase 6 — Historical Transaction Analysis & Evidence Collection

Historical analysis uses only persisted `PaymentTransaction` rows and a configurable window anchored to the current payment's ISO 20022 creation time. The default is 90 days (`historical-analysis.window-days`, or `HISTORICAL_ANALYSIS_WINDOW_DAYS`); both ends are inclusive and the current database row is explicitly excluded. Queries are scoped by debtor and date, with additional repository methods for creditor and status filtering.

The analysis reports counts and most recent transaction, successful historical beneficiary accounts, recent success/failure counts and failure rate. Amount baselines use only successful payments for the same debtor and normalized beneficiary name in the current currency. Average, minimum, maximum and amount ratio use `BigDecimal`; currencies are never mixed. This is an amount anomaly indicator when Phase 4 has produced that finding, not a fraud conclusion or an explanation of failure.

The evidence collector emits typed `HISTORICAL_TRANSACTION`, `HISTORICAL_BENEFICIARY`, `HISTORICAL_AMOUNT`, and `HISTORICAL_FAILURE_PATTERN` records only when database facts support them. These can accompany separate Phase 4 validation evidence and retain the repository source reference. With no history, counts are zero, calculated amount values are absent, and no historical evidence is persisted.

`GET /api/v1/investigations/{transactionId}/history` returns structured analysis for debugging without creating a case. The normal investigation response keeps its existing Phase 4/5 shape. This deterministic, traceable layer prepares evidence for later agents; no AI or external model is called.

```powershell
mvn test
```

## Phase 7 — Evidence Agent & Risk Agent

Phase 7 adds deterministic agent contracts. The Evidence Agent classifies the persisted Phase 4/6 evidence, preserves evidence type/rule/source provenance, calculates an internal evidence-strength score, reports factual gaps and flags conflicting beneficiary history. The Risk Agent scores only real failed validation findings, creates explainable risk factors and separately calculates confidence from evidence strength and completeness. High/critical risk, insufficient evidence, low confidence, or conflicting history can require human review. Agents recommend; they do not execute banking actions or override existing Phase 4 response fields.

The score is a demonstration model, not an industry standard. Agent results are returned in the additive `agentAssessment` response field and are not persisted during Phase 7. No LLM or external AI service is used. See [`docs/ai-agents.md`](docs/ai-agents.md) for scoring, confidence, evidence quality and review rules.

## Phase 12 — Human Review Workflow

Human review is required for deterministic `REQUIRES_REVIEW` cases, HIGH/CRITICAL risk, insufficient or conflicting evidence, or Verification Agent rejection/review requirements. This eligibility marker is persisted on the case; low-risk clear cases are not automatically eligible. It records workflow metadata only and does not modify deterministic findings, risk level, evidence, history, reasoning, or verification output.

`POST /api/v1/investigations/cases/{caseId}/review` accepts `decision` (`APPROVE`, `REJECT`, or `ESCALATE`), a nonblank reviewer, and a required analyst reason/comment (5–2000 characters). The service locks the case, checks eligibility and valid transitions, updates only workflow status, and inserts an immutable audit row. `GET /api/v1/investigations/cases/{caseId}/reviews` returns the append-only decision history in chronological order. Unknown cases return 404, invalid requests return 400, and invalid/ineligible transitions return 409.

Flyway migration `V5__create_investigation_review.sql` adds the persisted review eligibility marker and `investigation_review` audit table with a case foreign key and chronological index. Decisions move eligible `CLEAR`, `REQUIRES_REVIEW`, `OPEN`, or `UNDER_REVIEW` cases to their selected decision state. `ESCALATED` cases can be approved or rejected; final `APPROVED`, `REJECTED`, and `CLOSED` states reject additional decisions. Each review retains the prior and resulting status, reviewer, reason, and timestamps.

The React investigation detail page exposes decisions only when the current case requires human review. It posts decisions to the backend, then reloads the persisted case and review history. The dashboard does not infer or change risk or claim a decision was saved before the API confirms it.
