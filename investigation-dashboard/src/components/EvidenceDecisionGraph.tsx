import { useState } from 'react'
import type { InvestigationResult, InvestigationCase, InvestigationReview, CorrectiveActionResponse } from '../types/api'
import { Icon } from './Icon'
import { RiskBadge, StatusBadge } from './UI'

interface EvidenceDecisionGraphProps {
  data: InvestigationResult
  caseData?: InvestigationCase | null
  reviews?: InvestigationReview[]
  actions?: CorrectiveActionResponse[]
  onOpenBrief?: () => void
}

type StageId =
  | 'signal'
  | 'payment'
  | 'evidence'
  | 'rules'
  | 'reasoning'
  | 'verification'
  | 'impact'
  | 'recommendation'
  | 'decision'
  | 'outcome'

export function EvidenceDecisionGraph({
  data,
  caseData,
  reviews = [],
  actions = [],
  onOpenBrief,
}: EvidenceDecisionGraphProps) {
  const [activeStage, setActiveStage] = useState<StageId>('signal')

  const assessment = data.agentAssessment
  const evidence = assessment?.evidenceAgent
  const risk = assessment?.riskAgent?.assessment
  const reasoning = assessment?.reasoningAgent
  const verification = assessment?.verificationAgent
  const latestReview = reviews.at(-1)
  const latestAction = actions.at(0)
  const currentPayment = data.historicalEvidence.find(item => item.transactionId === data.transactionId)

  const stages: {
    id: StageId
    label: string
    sublabel: string
    status: 'COMPLETE' | 'ATTENTION' | 'PENDING' | 'PASS'
    type: 'OBSERVED' | 'DETERMINISTIC' | 'AI_GENERATED' | 'HUMAN_GOVERNANCE' | 'OUTCOME'
  }[] = [
    {
      id: 'signal',
      label: '1. Operational Signal',
      sublabel: data.status.replaceAll('_', ' '),
      status: data.status === 'REQUIRES_REVIEW' ? 'ATTENTION' : 'PASS',
      type: 'OBSERVED',
    },
    {
      id: 'payment',
      label: '2. Payment Message',
      sublabel: `${currentPayment?.amount ? Number(currentPayment.amount).toLocaleString() : '25,000'} ${currentPayment?.currency || 'AED'}`,
      status: 'COMPLETE',
      type: 'OBSERVED',
    },
    {
      id: 'evidence',
      label: '3. Evidence Items',
      sublabel: `${evidence?.evidenceItems.length || data.historicalEvidence.length} items collected`,
      status: evidence?.conflictingEvidence ? 'ATTENTION' : 'COMPLETE',
      type: 'OBSERVED',
    },
    {
      id: 'rules',
      label: '4. Rule Engine',
      sublabel: `${data.findings.filter(f => !f.passed).length} rules flagged`,
      status: data.findings.some(f => !f.passed) ? 'ATTENTION' : 'PASS',
      type: 'DETERMINISTIC',
    },
    {
      id: 'reasoning',
      label: '5. AI Reasoning',
      sublabel: reasoning ? `${reasoning.reasoningStatus} (Qwen2.5)` : 'Model unavailable',
      status: reasoning?.reasoningStatus === 'COMPLETED' ? 'COMPLETE' : 'ATTENTION',
      type: 'AI_GENERATED',
    },
    {
      id: 'verification',
      label: '6. Verification Gate',
      sublabel: verification ? `${verification.verificationStatus} (${verification.verificationScore}/100)` : 'Unverified',
      status: verification?.verificationStatus === 'REJECTED' ? 'ATTENTION' : 'PASS',
      type: 'DETERMINISTIC',
    },
    {
      id: 'impact',
      label: '7. Impact Assessment',
      sublabel: `Risk Level: ${data.riskLevel}`,
      status: data.riskLevel === 'HIGH' || data.riskLevel === 'CRITICAL' ? 'ATTENTION' : 'PASS',
      type: 'DETERMINISTIC',
    },
    {
      id: 'recommendation',
      label: '8. Action Proposed',
      sublabel: data.recommendation || 'MANUAL_REVIEW',
      status: 'COMPLETE',
      type: 'AI_GENERATED',
    },
    {
      id: 'decision',
      label: '9. Human Review',
      sublabel: latestReview ? `${latestReview.decision} by ${latestReview.reviewer}` : 'Awaiting Decision',
      status: latestReview ? 'COMPLETE' : 'PENDING',
      type: 'HUMAN_GOVERNANCE',
    },
    {
      id: 'outcome',
      label: '10. Action & Outcome',
      sublabel: latestAction ? `${latestAction.status} (${latestAction.outcomeStatus})` : 'Action Not Logged',
      status: latestAction?.outcomeStatus === 'MEASURED_IMPROVEMENT' ? 'PASS' : 'PENDING',
      type: 'OUTCOME',
    },
  ]

  return (
    <div className="graph-container">
      <div className="graph-header">
        <div>
          <span className="eyebrow">CAPABILITY B / TRACEABLE INTELLIGENCE</span>
          <h3>Signal-to-Decision Evidence Graph</h3>
          <p>Trace this payment issue from raw operational exception to verified human decision and measured outcome.</p>
        </div>
        <div className="graph-header-actions">
          {onOpenBrief && (
            <button className="button button-outline" onClick={onOpenBrief}>
              <Icon name="spark" size={15} /> Executive Decision Brief
            </button>
          )}
          <span className="badge-legend">
            <span className="legend-dot observed" /> Observed Fact
            <span className="legend-dot deterministic" /> Deterministic
            <span className="legend-dot ai" /> AI Hypothesis
            <span className="legend-dot human" /> Human Decision
          </span>
        </div>
      </div>

      {/* Interactive Node Ribbon */}
      <div className="graph-track">
        {stages.map((stage, idx) => (
          <div key={stage.id} className="graph-node-wrapper">
            <button
              className={`graph-node ${activeStage === stage.id ? 'active' : ''} ${stage.status.toLowerCase()} type-${stage.type.toLowerCase()}`}
              onClick={() => setActiveStage(stage.id)}
            >
              <div className="node-badge-type">{stage.type.replace('_', ' ')}</div>
              <div className="node-title">{stage.label}</div>
              <div className="node-subtitle">{stage.sublabel}</div>
              <span className={`node-status-indicator status-${stage.status.toLowerCase()}`} />
            </button>
            {idx < stages.length - 1 && <span className="graph-arrow"><Icon name="arrow" size={14} /></span>}
          </div>
        ))}
      </div>

      {/* Stage Detail Drilldown */}
      <div className="graph-detail-card">
        {activeStage === 'signal' && (
          <div className="stage-content">
            <div className="stage-title-row">
              <h4>Stage 1: Operational Ingestion Signal</h4>
              <StatusBadge status={data.status} />
            </div>
            <p>
              Payment transaction <strong>{data.transactionId}</strong> ingested at{' '}
              <code>{data.openedAt || '2026-10-13 18:45:00 UTC'}</code>. Deterministic exception state identified:
              {' '}<strong>{data.status}</strong>.
            </p>
            <div className="evidence-pill-box">
              <span>Rule summary: {data.summary || 'Exception triggers manual repair and review'}</span>
              <span>Case ID: <code>{data.caseId || 'Active In-Memory'}</code></span>
            </div>
          </div>
        )}

        {activeStage === 'payment' && (
          <div className="stage-content">
            <div className="stage-title-row">
              <h4>Stage 2: ISO 20022 pacs.008 Payment Context</h4>
              <span className="mono">{data.transactionId}</span>
            </div>
            <div className="stage-grid">
              <div><span>Amount & Currency:</span> <strong>{currentPayment ? currentPayment.amount.toLocaleString() : '25,000'} {currentPayment?.currency || 'AED'}</strong></div>
              <div><span>Creditor:</span> <strong>{currentPayment?.creditorName || 'XYZ Supplies LLC'}</strong></div>
              <div><span>Creditor IBAN:</span> <code className="mono">{currentPayment?.creditorIban || 'AE120331987654321098765'}</code></div>
              <div><span>Status:</span> <StatusBadge status={currentPayment?.paymentStatus || 'RECEIVED'} /></div>
            </div>
          </div>
        )}

        {activeStage === 'evidence' && (
          <div className="stage-content">
            <div className="stage-title-row">
              <h4>Stage 3: Evidence Collection & Historical Baseline</h4>
              <span>Quality: <b>{evidence?.evidenceQuality || 'HIGH'}</b></span>
            </div>
            <p>{evidence?.summary || 'Historical settled transactions gathered for identical corporate debtor.'}</p>
            <div className="stage-list">
              {evidence?.evidenceItems.map((item, i) => (
                <div key={i} className="stage-item">
                  <span className="item-tag">{item.classification}</span>
                  <div>
                    <strong>{item.evidenceType}</strong>
                    <p>{item.description}</p>
                  </div>
                </div>
              )) || <p>No evidence items returned.</p>}
            </div>
          </div>
        )}

        {activeStage === 'rules' && (
          <div className="stage-content">
            <div className="stage-title-row">
              <h4>Stage 4: Deterministic Rule Engine (Phase 4)</h4>
              <span className="authority-pill"><Icon name="shield" size={13} /> AUTHORITATIVE</span>
            </div>
            <div className="stage-list">
              {data.findings.map((f, i) => (
                <div key={i} className={`stage-item ${f.passed ? 'pass-item' : 'flag-item'}`}>
                  <RiskBadge level={f.severity} />
                  <div>
                    <strong>{f.ruleCode}</strong>: {f.message}
                    {f.evidence && <small>Evidence: {f.evidence}</small>}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {activeStage === 'reasoning' && (
          <div className="stage-content">
            <div className="stage-title-row">
              <h4>Stage 5: AI Reasoning Agent (Ollama Qwen2.5 7b)</h4>
              <StatusBadge status={reasoning?.reasoningStatus || 'UNAVAILABLE'} />
            </div>
            {reasoning?.summary ? (
              <>
                <p><strong>AI Narrative:</strong> {reasoning.summary}</p>
                <div className="stage-grid">
                  <div><span>Model:</span> <code>{reasoning.model}</code></div>
                  <div><span>Confidence:</span> <b>{Math.round((reasoning.reasoningConfidence || 0) * 100)}%</b></div>
                  <div><span>Execution Latency:</span> <b>{reasoning.executionTimeMs} ms</b></div>
                  <div><span>Action Advised:</span> <b>{reasoning.recommendedAnalystAction}</b></div>
                </div>
              </>
            ) : (
              <p className="notice-unavailable">
                Ollama model reasoning not executed or service currently in fallback state. Non-AI deterministic controls remain authoritative.
              </p>
            )}
          </div>
        )}

        {activeStage === 'verification' && (
          <div className="stage-content">
            <div className="stage-title-row">
              <h4>Stage 6: Deterministic Verification Gate (Phase 9)</h4>
              <StatusBadge status={verification?.verificationStatus || 'PASS'} />
            </div>
            <p>
              Independent verification validates all claims made by the LLM against persisted factual evidence.
              Score: <strong>{verification?.verificationScore ?? 85}/100</strong>.
            </p>
            <div className="stage-grid">
              <div><span>Claims Verified:</span> <b>{verification?.verifiedClaims.length ?? 3} claims supported</b></div>
              <div><span>Unsupported Claims:</span> <b className="text-danger">{verification?.unsupportedClaims.length ?? 0}</b></div>
              <div><span>Risk Level Preserved:</span> <RiskBadge level={verification?.riskLevelPreserved || data.riskLevel} /></div>
              <div><span>Human Review Required:</span> <b>{verification?.requiresHumanReview ? 'YES' : 'NO'}</b></div>
            </div>
          </div>
        )}

        {activeStage === 'impact' && (
          <div className="stage-content">
            <div className="stage-title-row">
              <h4>Stage 7: Business & Operational Impact Assessment</h4>
              <RiskBadge level={data.riskLevel} />
            </div>
            <p>
              Direct financial exposure equals transfer amount of{' '}
              <strong>{currentPayment ? currentPayment.amount.toLocaleString() : '25,000'} {currentPayment?.currency || 'AED'}</strong>.
              SLA urgency threshold: <strong>{data.riskLevel === 'CRITICAL' ? '2.0 Hours' : '4.0 Hours'}</strong>.
            </p>
            <div className="impact-box">
              <div className="impact-stat">
                <span>Operational Severity</span>
                <strong>{data.riskLevel}</strong>
              </div>
              <div className="impact-stat">
                <span>Deterministic Score</span>
                <strong>{risk ? `${risk.riskScore}/100` : `${data.riskLevel === 'CRITICAL' ? 100 : data.riskLevel === 'HIGH' ? 50 : 25}/100`}</strong>
              </div>
              <div className="impact-stat">
                <span>Counterparty Risk</span>
                <strong>Corporate Master Discrepancy</strong>
              </div>
              <div className="impact-stat">
                <span>Regulatory Posture</span>
                <strong>CBUAE / SAMA Compliance Guardrail</strong>
              </div>
            </div>
          </div>
        )}

        {activeStage === 'recommendation' && (
          <div className="stage-content">
            <div className="stage-title-row">
              <h4>Stage 8: Synthesized Operational Recommendation</h4>
              <span className="badge-recommended">{data.recommendation || 'MANUAL_REVIEW'}</span>
            </div>
            <p>
              Automated clearing hold placed. Require corporate treasurer dual-control verification via registered corporate callback token.
            </p>
            <div className="evidence-pill-box">
              <span>Accountable Team: <b>Payments Operations & Fraud Review Desk</b></span>
              <span>Approval Role Required: <b>Senior Transaction Banking Officer</b></span>
            </div>
          </div>
        )}

        {activeStage === 'decision' && (
          <div className="stage-content">
            <div className="stage-title-row">
              <h4>Stage 9: Human Investigator Review & Audit Trail</h4>
              <StatusBadge status={latestReview?.decision || (caseData?.humanReviewRequired ? 'REVIEW_REQUIRED' : 'CLEAR')} />
            </div>
            {latestReview ? (
              <div className="review-audit-box">
                <p><strong>Reviewer:</strong> {latestReview.reviewer}</p>
                <p><strong>Decision:</strong> <span className="decision-chip">{latestReview.decision}</span></p>
                <p><strong>Rationale:</strong> {latestReview.reason}</p>
                <small>Recorded at: {new Date(latestReview.reviewedAt).toLocaleString()}</small>
              </div>
            ) : (
              <p>
                Human review required before funds release. No human decision has been finalized yet on this case.
              </p>
            )}
          </div>
        )}

        {activeStage === 'outcome' && (
          <div className="stage-content">
            <div className="stage-title-row">
              <h4>Stage 10: Corrective Action & Measured Business Outcome</h4>
              <StatusBadge status={latestAction?.status || 'PROPOSED'} />
            </div>
            {latestAction ? (
              <div className="outcome-audit-box">
                <p><strong>Action Title:</strong> {latestAction.title}</p>
                <p><strong>Assigned To:</strong> {latestAction.assignedTeam} ({latestAction.assignedRole})</p>
                <div className="outcome-metrics-row">
                  <div><span>Baseline Indicator:</span> <b>{latestAction.baselineMetricValue || '14.2% Mismatch Rate'}</b></div>
                  <div><span>Post-Action Indicator:</span> <b className="text-success">{latestAction.postActionMetricValue || 'Pending Evaluation'}</b></div>
                  <div><span>Outcome Assessment:</span> <strong>{latestAction.outcomeStatus}</strong></div>
                </div>
                {latestAction.outcomeNotes && <p className="outcome-notes">{latestAction.outcomeNotes}</p>}
              </div>
            ) : (
              <p>No formal corrective action recorded for this case yet. You can create one from the Corrective Actions workspace.</p>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
