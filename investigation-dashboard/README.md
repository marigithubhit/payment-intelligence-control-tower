# ISO 20022 AI Investigation Dashboard

An executive-oriented banking operations interface for reviewing payment investigations, deterministic findings, evidence, risk, AI reasoning, verification and curated knowledge. The dashboard uses only APIs implemented by the Spring Boot backend. It does not claim production readiness or regulatory certification.

## Technology

- React 19 and TypeScript
- Vite 6
- React Router
- Modern responsive CSS

## Folder structure

```text
investigation-dashboard/
├── public/
├── src/
│   ├── api/            Centralized typed backend clients
│   ├── components/     Reusable interface components
│   ├── types/          Types matched to Spring response DTOs
│   ├── App.tsx
│   ├── main.tsx
│   └── styles.css
├── .env.example
├── package.json
└── README.md
```

## Start the backend

From the project root (`C:\iso20022-ai-investigation`):

```powershell
mvn spring-boot:run
```

The backend defaults to `http://localhost:8080` and uses the existing local PostgreSQL and Ollama configuration.

## Start the frontend

```powershell
cd C:\iso20022-ai-investigation\investigation-dashboard
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173).

## Environment variables

Copy `.env.example` to `.env.local` to override the backend URL:

```dotenv
VITE_API_BASE_URL=http://localhost:8080
```

No secrets or credentials are stored in the frontend.

## Dashboard views

- **Dashboard:** backend health, honest unavailable aggregate metrics, and transaction lookup.
- **Investigations:** search by transaction ID; investigation detail displays the backend's deterministic findings, historical evidence, evidence/risk agent results, reasoning, verification and persisted case evidence.
- **Human review:** eligible investigation detail pages show actual persisted review state, a decision dialog requiring reviewer/reason, and the backend's append-only review history. The UI refreshes the saved case and audit history after a decision.
- **Payments:** submit XML through the existing pacs.008 ingestion endpoint and view its response.
- **Knowledge:** search the internal curated knowledge records and see each document code, source identifier and version.
- Sidebar Evidence, AI Reasoning and Verification entries lead to transaction lookup because the backend exposes those results inside an investigation response, not separate list endpoints.

## Backend API dependencies detected from Java controllers and DTOs

- `GET /api/v1/health`
- `POST /api/v1/payments/iso20022` (`application/xml`)
- `GET /api/v1/investigations/{transactionId}`
- `GET /api/v1/investigations/cases/{caseId}`
- `GET /api/v1/investigations/{transactionId}/history`
- `POST /api/v1/agents/reasoning` (`application/json`)
- `POST /api/v1/agents/verification` (`application/json`)
- `GET /api/v1/knowledge/search?q={query}`
- `POST /api/v1/investigations/cases/{caseId}/review`
- `GET /api/v1/investigations/cases/{caseId}/reviews`

There are no standalone Evidence Agent or Risk Agent controllers, no aggregate investigation/activity endpoint, no payment-by-ID read endpoint, and no raw-XML retrieval endpoint. The UI does not invent these APIs or data. Investigation detail reads agent results embedded in `InvestigationResult.agentAssessment`.

Backend errors are represented by `ApiError`: `timestamp`, `status`, `code`, and `message`. The frontend displays the message without exposing stack traces.

## Testing

```powershell
npm run build
```

For a live end-to-end check, run the backend and frontend in separate terminals, confirm backend status in the sidebar, load a transaction ID, and search `beneficiary`, `pacs.008`, `IntrBkSttlmAmt`, and an unknown term in Knowledge. The browser frontend uses synthetic-data safeguards; only submit payment data you are authorized to send.
