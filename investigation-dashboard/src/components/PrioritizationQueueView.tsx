import { useState } from 'react'
import type { PriorityQueueItem } from '../types/api'
import { Icon } from './Icon'
import { Panel, RiskBadge, SectionHeading } from './UI'

interface PrioritizationQueueViewProps {
  items: PriorityQueueItem[]
  loading: boolean
  onSelectTransaction: (id: string) => void
  onOpenBrief: (id: string) => void
}

export function PrioritizationQueueView({
  items,
  loading,
  onSelectTransaction,
  onOpenBrief,
}: PrioritizationQueueViewProps) {
  const [filterSeverity, setFilterSeverity] = useState<string>('ALL')
  const [filterSla, setFilterSla] = useState<string>('ALL')

  if (loading) {
    return (
      <div className="page">
        <div className="loading-view">
          <span className="loader" />
          <div>
            <strong>Calculating Executive Prioritization Queue</strong>
            <p>Evaluating multi-factor defensible priority scores across open payment cases...</p>
          </div>
        </div>
      </div>
    )
  }

  const filteredItems = items.filter(item => {
    if (filterSeverity !== 'ALL' && item.severity !== filterSeverity) return false
    if (filterSla !== 'ALL' && item.slaStatus !== filterSla) return false
    return true
  })

  return (
    <div className="page priority-queue-page">
      <div className="page-title-row">
        <div>
          <div className="eyebrow">CAPABILITY E / TRANSPARENT RANKING</div>
          <h1>Executive Attention & Prioritization Queue</h1>
          <p>
            Algorithmic, defensible prioritization queue showing which material payment issues demand immediate leadership review and why.
          </p>
        </div>
        <span className="demo-badge-pill">
          <Icon name="shield" size={14} /> EXPLAINABLE SCORING MODEL
        </span>
      </div>

      {/* Prioritization Formula Explanation */}
      <div className="formula-banner">
        <div className="formula-icon"><Icon name="spark" size={20} /></div>
        <div className="formula-content">
          <strong>Transparent Multi-Factor Scoring Formula (0 – 100 Points)</strong>
          <p>
            <strong>Priority Score</strong> = Operational Severity (max 40 pts) + SLA Urgency (max 30 pts) +
            Counterparty Concentration (max 15 pts) + Verification Contradiction / Conflict (max 15 pts).
          </p>
        </div>
      </div>

      {/* Filters */}
      <div className="filter-bar">
        <div className="filter-group">
          <span>Filter Severity:</span>
          {['ALL', 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'].map(sev => (
            <button
              key={sev}
              className={`filter-btn ${filterSeverity === sev ? 'active' : ''}`}
              onClick={() => setFilterSeverity(sev)}
            >
              {sev}
            </button>
          ))}
        </div>
        <div className="filter-group">
          <span>SLA Urgency:</span>
          {['ALL', 'BREACHED', 'AT_RISK', 'ON_TRACK'].map(s => (
            <button
              key={s}
              className={`filter-btn ${filterSla === s ? 'active' : ''}`}
              onClick={() => setFilterSla(s)}
            >
              {s.replace('_', ' ')}
            </button>
          ))}
        </div>
      </div>

      {/* Priority Table */}
      <Panel className="priority-table-panel">
        <SectionHeading
          eyebrow="QUEUE RANKING"
          title={`Ranked Action Items (${filteredItems.length})`}
          detail="Click 'Decision Brief' for executive summary or 'Investigate' to inspect underlying evidence."
        />
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Rank</th>
                <th>Transaction</th>
                <th>Priority Score</th>
                <th>Severity</th>
                <th>SLA Status</th>
                <th>Time in Queue</th>
                <th>Debtor & Creditor</th>
                <th>Amount</th>
                <th>Accountable Team</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredItems.map((item, idx) => (
                <tr key={item.transactionId} className={idx === 0 ? 'top-priority-row' : ''}>
                  <td>
                    <span className={`rank-badge ${idx < 3 ? 'rank-top' : ''}`}>#{idx + 1}</span>
                  </td>
                  <td>
                    <strong className="mono">{item.transactionId}</strong>
                    <small className="d-block text-muted">{item.market} Corridor</small>
                  </td>
                  <td>
                    <div className="score-cell">
                      <strong className="score-number">{item.priorityScore}</strong>
                      <span className="score-max">/100</span>
                      <div
                        className="score-breakdown-tooltip"
                        title={`Severity: ${item.scoreBreakdown.operationalSeverityPoints}pts | SLA: ${item.scoreBreakdown.slaUrgencyPoints}pts | Concentration: ${item.scoreBreakdown.volumeConcentrationPoints}pts | Verification: ${item.scoreBreakdown.verificationConflictPoints}pts`}
                      >
                        <Icon name="alert" size={13} />
                      </div>
                    </div>
                  </td>
                  <td>
                    <RiskBadge level={item.severity} />
                  </td>
                  <td>
                    <span className={`sla-chip sla-${item.slaStatus.toLowerCase().replace('_', '-')}`}>
                      {item.slaStatus.replace('_', ' ')}
                    </span>
                  </td>
                  <td>{item.hoursInQueue} hrs</td>
                  <td>
                    <div className="counterparty-cell">
                      <span>{item.debtorName}</span>
                      <small className="text-muted"><Icon name="arrow" size={11} /> {item.creditorName}</small>
                    </div>
                  </td>
                  <td>
                    <strong>{item.amount.toLocaleString(undefined, { minimumFractionDigits: 2 })} {item.currency}</strong>
                  </td>
                  <td>
                    <span className="team-chip">{item.assignedTeam}</span>
                  </td>
                  <td>
                    <div className="action-buttons-cell">
                      <button
                        className="button button-primary small"
                        onClick={() => onOpenBrief(item.transactionId)}
                        title="Open Executive AI Decision Brief"
                      >
                        <Icon name="spark" size={13} /> Brief
                      </button>
                      <button
                        className="button button-outline small"
                        onClick={() => onSelectTransaction(item.transactionId)}
                        title="Open Investigation Detail"
                      >
                        <Icon name="search" size={13} /> Case
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Panel>
    </div>
  )
}
