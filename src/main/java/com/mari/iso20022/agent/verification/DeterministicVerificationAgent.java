package com.mari.iso20022.agent.verification;

import com.mari.iso20022.agent.evidence.AssessedEvidenceItem;
import com.mari.iso20022.agent.reasoning.AnalystAction;
import com.mari.iso20022.investigation.dto.RiskLevel;
import com.mari.iso20022.validation.ValidationFinding;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Deterministic safety checks only; this agent never calls an LLM or changes deterministic risk. */
@Service
public class DeterministicVerificationAgent implements VerificationAgent {
    private static final Set<String> STOP_WORDS = Set.of("a", "an", "and", "are", "as", "at", "be", "by",
            "from", "for", "in", "is", "it", "of", "on", "or", "that", "the", "this", "to", "was",
            "were", "with", "than", "has", "have", "used", "uses", "payment", "payments", "current",
            "supplied", "evidence", "indicates", "shows", "based", "data");
    private static final Pattern PROMPT_INJECTION = Pattern.compile(
            "(?i)ignore\\s+(?:all\\s+)?(?:previous|prior)\\s+instructions[^.]*[.]?");
    private static final Set<String> UNSUPPORTED_ALLEGATIONS = Set.of(
            "fraud", "fraudulent", "intentionally", "intentional", "knowingly", "malicious",
            "stolen", "theft", "launder", "moneylaundering", "criminal");

    @Override
    public VerificationAgentResponse verify(VerificationAgentRequest request) {
        List<VerificationFinding> findings = new ArrayList<>();
        List<String> verifiedClaims = new ArrayList<>();
        List<String> unsupportedClaims = new ArrayList<>();
        LinkedHashSet<String> verifiedReferences = new LinkedHashSet<>();
        Map<String, AssessedEvidenceItem> evidenceById = evidenceCatalog(request.evidenceItems());
        var reasoning = request.reasoningResponse();

        if (reasoning == null || !Objects.equals(request.transactionId(), reasoning.transactionId())) {
            add(findings, "TRANSACTION_ID_MISMATCH", "CRITICAL",
                    "Reasoning response transaction ID does not match the verification request.", List.of());
        }

        if (reasoning == null || reasoning.reasoningStatus() != com.mari.iso20022.agent.reasoning.ReasoningStatus.AVAILABLE) {
            add(findings, "INSUFFICIENT_EVIDENCE", "HIGH",
                    "No completed reasoning response is available to verify.", List.of());
        }

        List<String> allReferences = collectReferences(reasoning);
        for (String id : allReferences) {
            if (!evidenceById.containsKey(id)) {
                add(findings, "HALLUCINATED_EVIDENCE_REFERENCE", "CRITICAL",
                        "Reasoning refers to an evidence ID that was not supplied.", List.of(id));
            } else {
                verifiedReferences.add(id);
            }
        }

        if (reasoning != null) {
            for (var fact : reasoning.observedFacts()) {
                List<String> ids = fact == null ? List.of() : Collections.singletonList(fact.evidenceId());
                List<AssessedEvidenceItem> sources = knownEvidence(ids, evidenceById);
                if (fact == null || blank(fact.fact()) || sources.isEmpty() || !supports(fact.fact(), sources, request)) {
                    String claim = fact == null ? "(empty observed fact)" : fact.fact();
                    unsupportedClaims.add(claim);
                    add(findings, sources.isEmpty() ? "EVIDENCE_NOT_TRACEABLE" : "UNSUPPORTED_CLAIM", "HIGH",
                            "Observed fact is not traceable to supplied evidence.", ids);
                } else {
                    verifiedClaims.add(fact.fact());
                }
            }
            for (var statement : reasoning.reasoning()) {
                List<String> ids = statement == null || statement.evidenceIds() == null ? List.of() : statement.evidenceIds();
                List<AssessedEvidenceItem> sources = knownEvidence(ids, evidenceById);
                if (statement == null || blank(statement.statement()) || sources.isEmpty()
                        || !supports(statement.statement(), sources, request)) {
                    String claim = statement == null ? "(empty reasoning statement)" : statement.statement();
                    unsupportedClaims.add(claim);
                    add(findings, sources.isEmpty() ? "EVIDENCE_NOT_TRACEABLE" : "UNSUPPORTED_CLAIM", "HIGH",
                            "Reasoning statement is not supported by its cited evidence and deterministic context.", ids);
                } else {
                    verifiedClaims.add(statement.statement());
                }
            }
            for (var assessment : reasoning.evidenceAssessment()) {
                List<String> ids = assessment == null || assessment.evidenceIds() == null
                        ? List.of() : assessment.evidenceIds();
                List<AssessedEvidenceItem> sources = knownEvidence(ids, evidenceById);
                if (assessment == null || blank(assessment.assessment()) || sources.isEmpty()
                        || !supports(assessment.assessment(), sources, request)) {
                    String claim = assessment == null ? "(empty evidence assessment)" : assessment.assessment();
                    unsupportedClaims.add(claim);
                    add(findings, sources.isEmpty() ? "EVIDENCE_NOT_TRACEABLE" : "UNSUPPORTED_CLAIM", "HIGH",
                            "Evidence assessment is not supported by its cited evidence.", ids);
                } else {
                    verifiedClaims.add(assessment.assessment());
                }
            }
            List<AssessedEvidenceItem> summarySources = knownEvidence(allReferences, evidenceById);
            if (!blank(reasoning.summary())) {
                if (summarySources.isEmpty() || !supports(reasoning.summary(), summarySources, request)) {
                    unsupportedClaims.add(reasoning.summary());
                    add(findings, "UNSUPPORTED_CLAIM", "HIGH",
                            "Reasoning summary is not supported by supplied evidence.", allReferences);
                } else {
                    verifiedClaims.add(reasoning.summary());
                }
            }
            if (reasoning.recommendedAnalystAction() == null
                    || rank(reasoning.recommendedAnalystAction()) < minimumAction(request.riskAssessment().riskLevel(),
                    request.riskAssessment().recommendedAction())) {
                add(findings, "RISK_ACTION_MISMATCH", "HIGH",
                        "Recommended analyst action is less cautious than deterministic risk requires.", List.of());
            }
            if (contradictsRisk(reasoning.summary(), request.riskAssessment().riskLevel())
                    || reasoning.reasoning().stream().filter(Objects::nonNull)
                    .anyMatch(s -> contradictsRisk(s.statement(), request.riskAssessment().riskLevel()))) {
                add(findings, "RISK_REASONING_CONTRADICTION", "HIGH",
                        "AI reasoning describes risk or review requirements contrary to deterministic risk.", List.of());
            }
        }

        if (confidenceMismatch(reasoning == null ? null : reasoning.reasoningConfidence(), request)) {
            add(findings, "CONFIDENCE_EVIDENCE_MISMATCH", "HIGH",
                    "Reasoning confidence is high despite insufficient or incomplete evidence.", List.of());
        }
        if (request.evidenceAssessment().conflictingEvidence()) {
            add(findings, "CONFLICTING_EVIDENCE", "HIGH",
                    "Evidence Agent identified conflicting evidence.", List.of());
        }
        if (insufficient(request)) {
            add(findings, "INSUFFICIENT_EVIDENCE", "MEDIUM",
                    "Evidence quality or completeness is insufficient for independent verification.", List.of());
        }
        if (validationInconsistent(request.validationFindings(), request.riskAssessment().riskLevel())) {
            add(findings, "VALIDATION_INCONSISTENCY", "HIGH",
                    "High-severity deterministic validation findings conflict with a LOW risk assessment.", List.of());
        }

        boolean hardReject = findings.stream().anyMatch(f -> Set.of("TRANSACTION_ID_MISMATCH",
                "HALLUCINATED_EVIDENCE_REFERENCE", "UNSUPPORTED_CLAIM", "EVIDENCE_NOT_TRACEABLE")
                .contains(f.code()));
        boolean needsHuman = findings.stream().anyMatch(f -> Set.of("RISK_ACTION_MISMATCH",
                "RISK_REASONING_CONTRADICTION", "CONFIDENCE_EVIDENCE_MISMATCH", "CONFLICTING_EVIDENCE",
                "INSUFFICIENT_EVIDENCE", "VALIDATION_INCONSISTENCY").contains(f.code()));
        VerificationStatus status = hardReject ? VerificationStatus.REJECTED
                : needsHuman ? VerificationStatus.REQUIRES_HUMAN_REVIEW
                : findings.isEmpty() ? VerificationStatus.VERIFIED : VerificationStatus.PARTIALLY_VERIFIED;
        boolean recommendationVerified = reasoning != null && reasoning.recommendedAnalystAction() != null
                && rank(reasoning.recommendedAnalystAction()) >= minimumAction(request.riskAssessment().riskLevel(),
                request.riskAssessment().recommendedAction());
        VerificationResult result = new VerificationResult(status, score(findings), findings,
                distinct(verifiedClaims), distinct(unsupportedClaims), List.copyOf(verifiedReferences),
                recommendationVerified, request.riskAssessment().riskLevel(),
                request.riskAssessment().requiresHumanReview() || needsHuman || hardReject);
        return VerificationAgentResponse.from(request.transactionId(), result);
    }

    private static Map<String, AssessedEvidenceItem> evidenceCatalog(List<AssessedEvidenceItem> items) {
        Map<String, AssessedEvidenceItem> catalog = new LinkedHashMap<>();
        for (int i = 0; i < items.size(); i++) catalog.put("E" + (i + 1), items.get(i));
        return catalog;
    }

    private static List<String> collectReferences(com.mari.iso20022.agent.reasoning.ReasoningAgentResponse response) {
        if (response == null) return List.of();
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (var fact : response.observedFacts()) if (fact != null && fact.evidenceId() != null) ids.add(fact.evidenceId());
        for (var item : response.evidenceAssessment()) if (item != null && item.evidenceIds() != null) ids.addAll(item.evidenceIds());
        for (var item : response.reasoning()) if (item != null && item.evidenceIds() != null) ids.addAll(item.evidenceIds());
        for (var ref : response.evidenceReferences()) if (ref != null && ref.evidenceId() != null) ids.add(ref.evidenceId());
        return List.copyOf(ids);
    }

    private static List<AssessedEvidenceItem> knownEvidence(List<String> ids, Map<String, AssessedEvidenceItem> catalog) {
        if (ids == null) return List.of();
        return ids.stream().filter(Objects::nonNull).map(catalog::get).filter(Objects::nonNull).toList();
    }

    private static boolean supports(String claim, List<AssessedEvidenceItem> sources, VerificationAgentRequest request) {
        String cleanClaim = stripInjection(claim);
        Set<String> claimTokens = tokens(cleanClaim);
        if (claimTokens.isEmpty()) return false;
        Set<String> sourceTokens = sources.stream().flatMap(item -> evidenceText(item).stream())
                .flatMap(text -> tokens(stripInjection(text)).stream()).collect(Collectors.toSet());
        for (ValidationFinding finding : request.validationFindings()) {
            sourceTokens.addAll(tokens(stripInjection(Objects.toString(finding.message(), ""))));
            sourceTokens.addAll(tokens(Objects.toString(finding.ruleCode(), "")));
            sourceTokens.addAll(tokens(stripInjection(Objects.toString(finding.evidence(), ""))));
        }
        var historical = request.historicalAnalysis();
        if (historical.historicalBeneficiaries() != null) {
            historical.historicalBeneficiaries().forEach(b -> sourceTokens.addAll(tokens(Objects.toString(b.creditorIban(), ""))));
        }
        sourceTokens.addAll(tokens(Objects.toString(request.riskAssessment().explanation(), "")));

        Set<String> allegations = new HashSet<>(claimTokens);
        allegations.retainAll(UNSUPPORTED_ALLEGATIONS);
        if (!allegations.isEmpty()) {
            Set<String> deterministicTokens = new HashSet<>(sourceTokens);
            deterministicTokens.retainAll(allegations);
            if (deterministicTokens.size() != allegations.size()) return false;
        }
        long supported = claimTokens.stream().filter(sourceTokens::contains).count();
        return supported >= Math.min(2, claimTokens.size())
                && (double) supported / claimTokens.size() >= 0.55;
    }

    private static List<String> evidenceText(AssessedEvidenceItem item) {
        return List.of(Objects.toString(item.evidenceType(), ""), Objects.toString(item.ruleCode(), ""),
                stripInjection(Objects.toString(item.description(), "")), Objects.toString(item.observedValue(), ""),
                Objects.toString(item.expectedValue(), ""));
    }

    private static Set<String> tokens(String value) {
        Set<String> result = new HashSet<>();
        for (String raw : value.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (raw.isBlank()) continue;
            String token = canonical(raw);
            if (token.length() > 1 && !STOP_WORDS.contains(token)) result.add(token);
        }
        return result;
    }

    private static String canonical(String token) {
        if (Set.of("different", "differs", "differed", "differing", "mismatch", "changed", "change").contains(token)) return "change";
        if (token.endsWith("s") && token.length() > 4) return token.substring(0, token.length() - 1);
        return token;
    }

    private static String stripInjection(String value) {
        return PROMPT_INJECTION.matcher(value).replaceAll(" ");
    }

    private static boolean confidenceMismatch(BigDecimal confidence, VerificationAgentRequest request) {
        return confidence != null && confidence.compareTo(new BigDecimal("0.70")) > 0
                && (request.evidenceAssessment().evidenceQuality().name().equals("INSUFFICIENT")
                || request.evidenceAssessment().evidenceCompleteness().name().equals("INSUFFICIENT"));
    }

    private static boolean insufficient(VerificationAgentRequest request) {
        return request.evidenceAssessment().evidenceQuality().name().equals("INSUFFICIENT")
                || request.evidenceAssessment().evidenceCompleteness().name().equals("INSUFFICIENT");
    }

    private static boolean contradictsRisk(String text, RiskLevel risk) {
        if (blank(text) || risk == RiskLevel.LOW) return false;
        String value = text.toLowerCase(Locale.ROOT).replace('-', ' ');
        return value.contains("low risk") || value.contains("no risk") || value.contains("no review")
                || value.contains("no action") || value.contains("safe to proceed") || value.contains("approve this payment");
    }

    private static boolean validationInconsistent(List<ValidationFinding> findings, RiskLevel risk) {
        return risk == RiskLevel.LOW && findings.stream().anyMatch(f -> !f.passed()
                && (f.severity().name().equals("HIGH") || f.severity().name().equals("CRITICAL")));
    }

    private static int minimumAction(RiskLevel risk, String deterministicAction) {
        int minimum = switch (risk) {
            case LOW -> rank(AnalystAction.MONITOR);
            case MEDIUM -> rank(AnalystAction.REVIEW);
            case HIGH -> rank(AnalystAction.MANUAL_REVIEW);
            case CRITICAL -> rank(AnalystAction.HOLD_AND_ESCALATE);
        };
        try { minimum = Math.max(minimum, rank(AnalystAction.valueOf(deterministicAction))); }
        catch (Exception ignored) { }
        return minimum;
    }

    private static int rank(AnalystAction action) { return action.ordinal(); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static List<String> distinct(List<String> values) { return List.copyOf(new LinkedHashSet<>(values)); }

    private static void add(List<VerificationFinding> findings, String code, String severity,
                            String message, List<String> ids) {
        if (findings.stream().noneMatch(f -> f.code().equals(code) && f.evidenceIds().equals(ids))) {
            findings.add(new VerificationFinding(code, severity, message, ids));
        }
    }

    private static int score(List<VerificationFinding> findings) {
        int value = 100;
        for (VerificationFinding finding : findings) {
            value -= switch (finding.code()) {
                case "TRANSACTION_ID_MISMATCH", "HALLUCINATED_EVIDENCE_REFERENCE" -> 50;
                case "UNSUPPORTED_CLAIM" -> finding.severity().equals("HIGH") ? 30 : 15;
                case "EVIDENCE_NOT_TRACEABLE" -> 50;
                case "RISK_ACTION_MISMATCH" -> 40;
                case "CONFIDENCE_EVIDENCE_MISMATCH" -> 20;
                case "RISK_REASONING_CONTRADICTION", "CONFLICTING_EVIDENCE", "VALIDATION_INCONSISTENCY" -> 30;
                case "INSUFFICIENT_EVIDENCE" -> finding.severity().equals("HIGH") ? 30 : 15;
                default -> 5;
            };
        }
        return Math.max(0, Math.min(100, value));
    }
}
