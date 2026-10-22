import { useState } from 'react'
import type { ExceptionPattern } from '../types/api'
import { Icon } from './Icon'
import { Panel, RiskBadge } from './UI'

interface ExceptionPatternsViewProps {
  patterns: ExceptionPattern[]
  loading: boolean
  onSelectTransaction: (id: string) => void
}

export function ExceptionPatternsView({
  patterns,
  loading,
  onSelectTransaction,
}: ExceptionPatternsViewProps) {
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL')

  if (loading) {
    return (
      <div className="page">
        <div className="loading-view">
          <span className="loader" />
          <div>
            <strong>Analyzing Payment Exception Patterns</strong>
            <p>Evaluating deterministic clusters across counterparty history, amounts, and rejection codes...</p>
          </div>
        </div>
      </div>
    )
  }

  const categories = ['ALL', ...Array.from(new Set(patterns.map(p => p.category)))]
  const filteredPatterns = patterns.filter(
    p => selectedCategory === 'ALL' || p.category === selectedCategory
  )

  const totalAffected = patterns.reduce((acc, p) => acc + p.affectedCount, 0)
  const totalValue = patterns.reduce((acc, p) => acc + p.affectedValueTotal, 0)

  return (
    <div className="page patterns-page">
      <div className="page-title-row">
        <div>
          <div className="eyebrow">CAPABILITY D / RECURRING ANOMALIES</div>
          <h1>Payment Exception Pattern Intelligence</h1>
          <p>
            Deterministic grouping and explainable clustering of recurring operational exceptions, beneficiary mismatches, and clearing drop-outs.
          </p>
        </div>
        <span className="demo-badge-pill">
          <Icon name="shield" size={14} /> DEMO DATA: EXPLAINABLE CLUSTERING
        </span>
      </div>

      {/* Pattern Summary Stats */}
      <div className="pattern-stat-banner">
        <div>
          <span>Detected Exception Clusters</span>
          <strong>{patterns.length} Recurring Patterns</strong>
        </div>
        <span className="stat-separator" />
        <div>
          <span>Affected Payment Transactions</span>
          <strong>{totalAffected} Transactions Flagged</strong>
        </div>
        <span className="stat-separator" />
        <div>
          <span>Aggregate Value at Exception</span>
          <strong>${(totalValue / 1000000).toFixed(2)}M Multi-Currency</strong>
        </div>
        <span className="stat-separator" />
        <div>
          <span>Detection Methodology</span>
          <strong>Deterministic Rules First (No Hallucinations)</strong>
        </div>
      </div>

      {/* Category Filter */}
      <div className="filter-bar">
        <span>Category Filter:</span>
        {categories.map(cat => (
          <button
            key={cat}
            className={`filter-btn ${selectedCategory === cat ? 'active' : ''}`}
            onClick={() => setSelectedCategory(cat)}
          >
            {cat.replace('_', ' ')}
          </button>
        ))}
      </div>

      {/* Pattern Cards List */}
      <div className="pattern-cards-stack">
        {filteredPatterns.map(pattern => (
          <Panel key={pattern.patternId} className="pattern-card">
            <div className="pattern-card-header">
              <div className="pattern-title-group">
                <div className="pattern-badges">
                  <span className="mono pattern-id">{pattern.patternId}</span>
                  <span className="category-pill">{pattern.category.replace('_', ' ')}</span>
                  <RiskBadge level={pattern.operationalSeverity as any} />
                </div>
                <h2>{pattern.patternName}</h2>
                <div className="pattern-meta-strip">
                  <span>Corridor: <b>{pattern.observedMarket}</b></span>
                  <span className="separator">·</span>
                  <span>Rule Trigger: <code>{pattern.ruleCode}</code></span>
                  <span className="separator">·</span>
                  <span>Affected: <strong>{pattern.affectedCount} Payments</strong></span>
                  <span className="separator">·</span>
                  <span>Exposure: <strong>${pattern.affectedValueTotal.toLocaleString()}</strong></span>
                </div>
              </div>
            </div>

            <div className="pattern-body-grid">
              <div className="pattern-block block-logic">
                <div className="block-title">
                  <Icon name="shield" size={15} />
                  <strong>Detection Logic</strong>
                </div>
                <p>{pattern.detectionLogic}</p>
              </div>

              <div className="pattern-block block-limitations">
                <div className="block-title">
                  <Icon name="alert" size={15} />
                  <strong>Confidence & Scope Limitations</strong>
                </div>
                <p>{pattern.confidenceLimitations}</p>
              </div>

              <div className="pattern-block block-action">
                <div className="block-title">
                  <Icon name="spark" size={15} />
                  <strong>Recommended Operational Next Step</strong>
                </div>
                <p>{pattern.recommendedNextStep}</p>
              </div>
            </div>

            {/* Affected Sample Transactions */}
            <div className="affected-samples-row">
              <span>Sample Flagged Payments:</span>
              <div className="sample-chips">
                {pattern.sampleTransactionIds.map(txnId => (
                  <button
                    key={txnId}
                    className="button button-outline small sample-txn-btn"
                    onClick={() => onSelectTransaction(txnId)}
                    title={`Open live investigation for ${txnId}`}
                  >
                    <Icon name="search" size={13} /> <span className="mono">{txnId}</span>
                  </button>
                ))}
              </div>
            </div>
          </Panel>
        ))}
      </div>
    </div>
  )
}
