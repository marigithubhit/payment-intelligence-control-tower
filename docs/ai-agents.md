# AI agents

The investigation flow runs three advisory agents in order: the deterministic Evidence Agent, the deterministic Risk Agent, then the optional local Ollama Reasoning Agent. Evidence and risk outputs remain authoritative. Reasoning enriches the API response and does not write to the database.

## Local reasoning model

Phase 8 uses Ollama's `/api/chat` endpoint with structured JSON output and a JSON Schema. Defaults are `http://localhost:11434`, `qwen2.5:7b`, temperature `0.1`, a 60 second timeout, and prompt version `1.0`. Set `OLLAMA_BASE_URL`, `OLLAMA_MODEL`, `OLLAMA_TEMPERATURE`, `OLLAMA_TIMEOUT_SECONDS`, or `REASONING_PROMPT_VERSION` to override them. Ollama and the model are optional at runtime; an unavailable or invalid response yields `UNAVAILABLE` or `REJECTED`, while deterministic results remain present.

The prompt treats payment names, remittance text, and other external strings as untrusted data. The response is schema-checked, must match the transaction, cite supplied evidence IDs, copy observed facts exactly, use only supplied uncertainty statements, and cannot weaken the deterministic recommended action. Reasoning confidence is separate from risk confidence and is capped at 0.50 when evidence quality is insufficient. It is not a fraud probability.

Logs contain transaction ID, configured model, prompt version, execution duration, and status. They do not contain prompts, payment fields, account identifiers, or raw model output.

Normal tests do not require Ollama. An optional local model integration check can be run with `mvn '-Dollama.integration=true' '-Dtest=OllamaReasoningIntegrationTest' test` when a responsive local Ollama server and configured model are available.
