import type { ExecutiveDecisionBrief } from '../types/api'
import { Icon } from './Icon'
import { RiskBadge, StatusBadge } from './UI'

interface ExecutiveDecisionBriefModalProps {
  brief: ExecutiveDecisionBrief | null
  onClose: () => void
  onNavigateToInvestigation?: (transactionId: string) => void
}

export function ExecutiveDecisionBriefModal({
  brief,
  onClose,
  onNavigateToInvestigation,
}: ExecutiveDecisionBriefModalProps) {
  if (!brief) return null

  return (
    <div className="brief-modal-backdrop" onClick={onClose}>
      <div className="brief-modal-content" onClick={e => e.stopPropagation()}>
        <div className="brief-modal-header">
          <div>
            <div className="eyebrow">CAPABILITY F / EXECUTIVE AI DECISION BRIEF</div>
            <h2>{brief.headline}</h2>
            <div className="brief-meta-line">
              <span className="mono">TXN: {brief.transactionId}</span>
              <span className="separator">·</span>
              <span>{brief.market}</span>
              <span className="separator">·</span>
              <span>
                <b>{brief.amount.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} {brief.currency}</b>
              </span>
              <span className="separator">·</span>
              <RiskBadge level={brief.preservedRiskLevel as any} />
            </div>
          </div>
          <button className="icon-button" onClick={onClose} aria-label="Close brief">
            <Icon name="close" size={18} />
          </button>
        </div>

        <div className="brief-modal-body">
          {/* Signal & Evidence */}
          <section className="brief-section">
            <div className="section-icon-title">
              <Icon name="search" size={16} />
              <h3>1. Observed Signal</h3>
            </div>
            <p className="brief-lead">{brief.observedSignal}</p>
          </section>

          <section className="brief-section">
            <div className="section-icon-title">
              <Icon name="layers" size={16} />
              <h3>2. Supporting Evidence</h3>
            </div>
            <ul className="brief-evidence-list">
              {brief.supportingEvidence.map((ev, i) => (
                <li key={i}><Icon name="check" size={14} /> <span>{ev}</span></li>
              ))}
            </ul>
          </section>

          {/* Duality: Establishes vs Uncertain */}
          <div className="brief-dual-grid">
            <div className="dual-card card-established">
              <div className="dual-title"><Icon name="shield" size={15} /> <strong>What Evidence Establishes</strong></div>
              <p>{brief.whatEvidenceEstablishes}</p>
            </div>
            <div className="dual-card card-uncertain">
              <div className="dual-title"><Icon name="alert" size={15} /> <strong>What Remains Uncertain</strong></div>
              <p>{brief.whatRemainsUncertain}</p>
            </div>
          </div>

          {/* Business Implication */}
          <section className="brief-section">
            <div className="section-icon-title">
              <Icon name="activity" size={16} />
              <h3>3. Business & Operational Implication</h3>
            </div>
            <div className="brief-callout implication-callout">
              <p>{brief.businessImplication}</p>
            </div>
          </section>

          {/* Recommended Next Step & Governance */}
          <section className="brief-section">
            <div className="section-icon-title">
              <Icon name="spark" size={16} />
              <h3>4. Recommended Operational Action & Governance</h3>
            </div>
            <div className="governance-grid">
              <div>
                <span>Recommended Action</span>
                <strong>{brief.recommendedNextStep}</strong>
              </div>
              <div>
                <span>Accountable Team</span>
                <strong>{brief.accountableTeam}</strong>
              </div>
              <div>
                <span>Human Approval Role</span>
                <strong>{brief.requiredApprovalRole}</strong>
              </div>
              <div>
                <span>Approval Required</span>
                <span className={`approval-badge ${brief.humanApprovalRequired ? 'req-yes' : 'req-no'}`}>
                  {brief.humanApprovalRequired ? 'MANDATORY' : 'AUTOMATED CLEARANCE'}
                </span>
              </div>
            </div>
          </section>

          {/* Verification & Reliability Controls */}
          <section className="brief-section">
            <div className="section-icon-title">
              <Icon name="shield" size={16} />
              <h3>5. Independent AI Verification Status</h3>
            </div>
            <div className="verification-strip">
              <div>
                <span>Verification Gate</span>
                <StatusBadge status={brief.verificationStatus} />
              </div>
              <div>
                <span>Verification Score</span>
                <strong>{brief.verificationScore} / 100</strong>
              </div>
              <div>
                <span>Preserved Risk Tier</span>
                <RiskBadge level={brief.preservedRiskLevel as any} />
              </div>
              <div>
                <span>AI Reasoning Engine</span>
                <span className="mono">{brief.liveReasoningUsed ? 'Ollama Qwen2.5 7b (Live)' : 'Deterministic Rules Fallback'}</span>
              </div>
            </div>
          </section>

          {/* Follow-up Measure */}
          <section className="brief-section">
            <div className="section-icon-title">
              <Icon name="clock" size={16} />
              <h3>6. Measurable Follow-Up Indicator</h3>
            </div>
            <p className="followup-text">{brief.followUpMeasure}</p>
          </section>
        </div>

        <div className="brief-modal-footer">
          <span className="disclaimer-note">
            <Icon name="shield" size={14} /> AI is advisory. Dual-analyst controls and deterministic verification are authoritative.
          </span>
          <div className="footer-action-buttons">
            <button className="button button-quiet" onClick={onClose}>Close Brief</button>
            {onNavigateToInvestigation && (
              <button
                className="button button-primary"
                onClick={() => {
                  onNavigateToInvestigation(brief.transactionId)
                  onClose()
                }}
              >
                Inspect Live Case <Icon name="arrow" size={15} />
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
