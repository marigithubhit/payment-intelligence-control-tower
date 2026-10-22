package com.mari.iso20022.agent.reasoning;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mari.iso20022.agent.evidence.EvidenceQuality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OllamaReasoningAgent implements ReasoningAgent {
    private static final Logger log = LoggerFactory.getLogger(OllamaReasoningAgent.class);
    private final OllamaClient client;
    private final ObjectMapper mapper;
    private final ReasoningPromptBuilder promptBuilder;
    private final OllamaProperties ollama;
    private final ReasoningProperties reasoning;

    public OllamaReasoningAgent(OllamaClient client, ObjectMapper mapper, ReasoningPromptBuilder promptBuilder,
                                OllamaProperties ollama, ReasoningProperties reasoning) {
        this.client = client; this.mapper = mapper; this.promptBuilder = promptBuilder;
        this.ollama = ollama; this.reasoning = reasoning;
    }

    @Override
    public ReasoningAgentResponse reason(ReasoningAgentRequest request) {
        long start = System.nanoTime();
        String promptVersion = reasoning.getPromptVersion();
        try {
            ReasoningPrompt prompt = promptBuilder.build(request);
            String content = client.complete(prompt);
            if (content == null || content.isBlank()) return rejected(request, start, "Model returned empty content");
            ReasoningResult result = mapper.readerFor(ReasoningResult.class)
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(content);
            String issue = validate(result, request, prompt);
            if (issue != null) {
                log.warn("reasoning output rejected transactionId={} model={} promptVersion={} validationIssue={} executionTimeMs={}",
                        request.transactionId(), ollama.getModel(), promptVersion, issue, elapsed(start));
                return rejected(request, start, issue);
            }
            boolean capped = request.evidenceAssessment().evidenceQuality() == EvidenceQuality.INSUFFICIENT
                    && result.confidence().compareTo(new BigDecimal("0.50")) > 0;
            BigDecimal confidence = capped ? new BigDecimal("0.50") : result.confidence();
            long duration = elapsed(start);
            log.info("reasoning agent completed transactionId={} model={} promptVersion={} executionTimeMs={} status=AVAILABLE",
                    request.transactionId(), ollama.getModel(), promptVersion, duration);
            return new ReasoningAgentResponse(request.transactionId(), ReasoningStatus.AVAILABLE, result.summary(),
                    result.observedFacts(), result.evidenceAssessment(), result.reasoning(), evidenceReferences(request), result.uncertainties(),
                    result.recommendedAnalystAction(), confidence, capped, ollama.getModel(), promptVersion,
                    duration, true, null);
        } catch (OllamaCallException ex) {
            long duration = elapsed(start);
            log.warn("reasoning agent unavailable transactionId={} model={} promptVersion={} executionTimeMs={}",
                    request.transactionId(), ollama.getModel(), promptVersion, duration);
            return ReasoningAgentResponse.unavailable(request.transactionId(), ollama.getModel(), promptVersion,
                    duration, "Ollama service unavailable");
        } catch (Exception ex) {
            Throwable cause = ex;
            while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
            log.warn("reasoning output processing failed transactionId={} model={} promptVersion={} failureType={} rootCauseType={} executionTimeMs={}",
                    request.transactionId(), ollama.getModel(), promptVersion, ex.getClass().getSimpleName(),
                    cause.getClass().getName(), elapsed(start));
            return rejected(request, start, "Model response failed schema or grounding validation");
        }
    }

    private String validate(ReasoningResult r, ReasoningAgentRequest q, ReasoningPrompt p) {
        if (r == null || !q.transactionId().equals(r.transactionId())) return "transactionId mismatch";
        if (blank(r.summary()) || r.observedFacts() == null || r.evidenceAssessment() == null || r.reasoning() == null
                || r.uncertainties() == null || r.summaryEvidenceIds() == null || r.recommendedAnalystAction() == null
                || r.confidence() == null || r.confidence().compareTo(BigDecimal.ZERO) < 0
                || r.confidence().compareTo(BigDecimal.ONE) > 0) return "missing or malformed required values";
        for (var f : r.observedFacts()) if (f == null || blank(f.evidenceId()) || !p.allowedFacts().containsKey(f.evidenceId())
                || !p.allowedFacts().get(f.evidenceId()).equals(f.fact())) return "ungrounded observed fact";
        for (var e : r.evidenceAssessment()) if (e == null || blank(e.assessment()) || e.importance() == null
                || !validRefs(e.evidenceIds(), p)) return "ungrounded evidence assessment";
        for (var s : r.reasoning()) if (s == null || blank(s.statement()) || !validRefs(s.evidenceIds(), p)) return "ungrounded reasoning";
        if (p.allowedEvidenceIds().isEmpty()) {
            if (!r.summaryEvidenceIds().isEmpty() || !r.summary().toLowerCase().contains("insufficient")) return "ungrounded summary";
        } else if (!validRefs(r.summaryEvidenceIds(), p)) return "ungrounded summary";
        if (r.uncertainties().stream().anyMatch(u -> !p.allowedUncertainties().contains(u))) return "unsupported uncertainty";
        AnalystAction deterministic;
        try { deterministic = AnalystAction.valueOf(q.riskAssessment().recommendedAction()); }
        catch (Exception ex) { return "unsupported deterministic recommendation"; }
        if (rank(r.recommendedAnalystAction()) < rank(deterministic)) return "action weakens deterministic recommendation";
        return null;
    }

    private static boolean validRefs(List<String> refs, ReasoningPrompt p) {
        return refs != null && !refs.isEmpty() && refs.stream().allMatch(id -> p.allowedEvidenceIds().contains(id));
    }
    private static boolean blank(String s) { return s == null || s.isBlank(); }
    private static List<ReasoningEvidenceReference> evidenceReferences(ReasoningAgentRequest request) {
        var items = request.evidenceItems();
        java.util.ArrayList<ReasoningEvidenceReference> references = new java.util.ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            var item = items.get(i);
            references.add(new ReasoningEvidenceReference("E" + (i + 1), item.evidenceType(), item.ruleCode(),
                    item.sourceReference(), item.description()));
        }
        return List.copyOf(references);
    }
    private static int rank(AnalystAction action) { return action.ordinal(); }
    private long elapsed(long start) { return (System.nanoTime() - start) / 1_000_000; }
    private ReasoningAgentResponse rejected(ReasoningAgentRequest request, long start, String error) {
        long duration = elapsed(start);
        log.warn("reasoning agent rejected transactionId={} model={} promptVersion={} executionTimeMs={}",
                request.transactionId(), ollama.getModel(), reasoning.getPromptVersion(), duration);
        return ReasoningAgentResponse.rejected(request.transactionId(), ollama.getModel(), reasoning.getPromptVersion(), duration, error);
    }
}
