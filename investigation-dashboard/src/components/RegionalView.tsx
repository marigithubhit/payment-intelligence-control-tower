import { useState } from 'react'
import type { RegionalIntelligence, MarketPerformance } from '../types/api'
import { Icon } from './Icon'
import { MetricCard, Panel, SectionHeading, StatusBadge } from './UI'

interface RegionalViewProps {
  data: RegionalIntelligence | null
  loading: boolean
  onSelectTransaction?: (id: string) => void
}

export function RegionalView({ data, loading, onSelectTransaction }: RegionalViewProps) {
  const [selectedMarketCode, setSelectedMarketCode] = useState<string>('ALL')
  const [currencyFilter, setCurrencyFilter] = useState<string>('ALL')

  if (loading) {
    return (
      <div className="page">
        <div className="loading-view">
          <span className="loader" />
          <div>
            <strong>Loading Regional Intelligence</strong>
            <p>Gathering performance metrics across UAE, Saudi Arabia, Bahrain, and South Africa corridors...</p>
          </div>
        </div>
      </div>
    )
  }

  if (!data) {
    return <div className="page"><p>Regional data currently unavailable.</p></div>
  }

  const filteredMarkets = data.markets.filter(m => {
    if (selectedMarketCode !== 'ALL' && m.marketCode !== selectedMarketCode) return false
    if (currencyFilter !== 'ALL' && m.currency !== currencyFilter) return false
    return true
  })

  const totalVolume = data.markets.reduce((acc, m) => acc + m.totalVolume, 0)
  const totalValue = data.markets.reduce((acc, m) => acc + m.totalValue, 0)
  const avgStp = (data.markets.reduce((acc, m) => acc + m.stpRate, 0) / data.markets.length).toFixed(1)
  const totalOpenInvestigations = data.markets.reduce((acc, m) => acc + m.openInvestigations, 0)

  return (
    <div className="page regional-page">
      <div className="page-title-row">
        <div>
          <div className="eyebrow">CAPABILITY C / MEA CORRIDORS</div>
          <h1>Regional Transaction Banking Intelligence</h1>
          <p>Cross-market performance, settlement rail health, and investigation ageing across Middle East & Africa.</p>
        </div>
        <span className="demo-badge-pill">
          <Icon name="shield" size={14} /> DEMO DATA: SYNTHETIC MEA CORRIDORS
        </span>
      </div>

      {/* Aggregate Regional Metrics */}
      <div className="metric-grid">
        <MetricCard
          label="Regional Processing Volume"
          value={totalVolume.toLocaleString()}
          icon="activity"
          caption="Tracked across 4 markets"
        />
        <MetricCard
          label="Estimated Settlement Value"
          value={`$${(totalValue / 1000000).toFixed(2)}M`}
          icon="credit"
          tone="blue"
          caption="Normalized multi-currency value"
        />
        <MetricCard
          label="Cross-Market STP Rate"
          value={`${avgStp}%`}
          icon="check"
          tone="green"
          caption="Straight-through processing"
        />
        <MetricCard
          label="Open Regional Investigations"
          value={totalOpenInvestigations.toString()}
          icon="alert"
          tone="amber"
          caption="In repair or analyst queues"
        />
      </div>

      {/* Corridor Filters */}
      <div className="filter-bar">
        <div className="filter-group">
          <span>Filter Market:</span>
          <button
            className={`filter-btn ${selectedMarketCode === 'ALL' ? 'active' : ''}`}
            onClick={() => setSelectedMarketCode('ALL')}
          >
            All Corridors ({data.markets.length})
          </button>
          {data.markets.map(m => (
            <button
              key={m.marketCode}
              className={`filter-btn ${selectedMarketCode === m.marketCode ? 'active' : ''}`}
              onClick={() => setSelectedMarketCode(m.marketCode)}
            >
              {m.country} ({m.currency})
            </button>
          ))}
        </div>
        <div className="filter-group">
          <span>Currency:</span>
          <select value={currencyFilter} onChange={e => setCurrencyFilter(e.target.value)} className="select-input">
            <option value="ALL">All Currencies</option>
            <option value="AED">AED (Emirates Dirham)</option>
            <option value="SAR">SAR (Saudi Riyal)</option>
            <option value="BHD">BHD (Bahraini Dinar)</option>
            <option value="ZAR">ZAR (South African Rand)</option>
          </select>
        </div>
      </div>

      {/* Market Cards */}
      <div className="market-cards-grid">
        {filteredMarkets.map(market => (
          <MarketCard key={market.marketCode} market={market} onSelectTransaction={onSelectTransaction} />
        ))}
      </div>

      {/* Payment Rails & Channels Breakdown */}
      <Panel className="rails-panel">
        <SectionHeading
          eyebrow="CLEARING INFRASTRUCTURE"
          title="Payment Rail & Channel Performance"
          detail="Active telemetry across RTGS, Instant Payment Systems (IPS), and multilateral clearing networks."
        />
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Rail Identifier</th>
                <th>Market</th>
                <th>Message Standard</th>
                <th>Processed Volume</th>
                <th>Avg Latency</th>
                <th>Rejection %</th>
                <th>Operational Status</th>
              </tr>
            </thead>
            <tbody>
              {data.rails.map(rail => (
                <tr key={rail.railCode}>
                  <td>
                    <strong>{rail.railCode}</strong>
                    <small className="d-block text-muted">{rail.railName}</small>
                  </td>
                  <td><span className="mono">{rail.marketCode}</span></td>
                  <td><code>{rail.messageType}</code></td>
                  <td>{rail.volume.toLocaleString()} txns</td>
                  <td>{rail.avgLatencyMs < 60 ? `${rail.avgLatencyMs} ms` : `${(rail.avgLatencyMs / 60).toFixed(1)} s`}</td>
                  <td>
                    <span className={rail.rejectionRate > 2.0 ? 'text-danger' : ''}>
                      {rail.rejectionRate}%
                    </span>
                  </td>
                  <td>
                    <StatusBadge status={rail.status} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Panel>

      <div className="disclaimer-callout">
        <Icon name="shield" size={16} />
        <div>
          <strong>Transparent Synthetic Data Disclosure</strong>
          <p>{data.syntheticDataNotice}</p>
        </div>
      </div>
    </div>
  )
}

function MarketCard({
  market,
  onSelectTransaction,
}: {
  market: MarketPerformance
  onSelectTransaction?: (id: string) => void
}) {
  return (
    <div className="market-card">
      <div className="market-card-head">
        <div>
          <span className="market-code-chip">{market.marketCode}</span>
          <h3>{market.country}</h3>
          <span className="market-currency">Primary Currency: <strong>{market.currency}</strong></span>
        </div>
        <StatusBadge status={market.operationalStatus} />
      </div>

      <div className="market-stat-row">
        <div>
          <span>STP Rate</span>
          <strong className="text-success">{market.stpRate}%</strong>
        </div>
        <div>
          <span>Rejection Rate</span>
          <strong className={market.rejectionRate > 3 ? 'text-danger' : ''}>{market.rejectionRate}%</strong>
        </div>
        <div>
          <span>SLA Adherence</span>
          <strong>{market.slaAdherencePercent}%</strong>
        </div>
        <div>
          <span>Open Cases</span>
          <strong className="text-amber">{market.openInvestigations}</strong>
        </div>
      </div>

      <div className="market-rails-tags">
        <span>Active Rails:</span>
        {market.activeRails.map((rail, i) => (
          <code key={i}>{rail}</code>
        ))}
      </div>

      <div className="market-primary-rule">
        <span>Dominant Exception Signal:</span>
        <div className="rule-badge">
          <Icon name="alert" size={13} />
          <code>{market.primaryExceptionCategory}</code>
        </div>
      </div>

      <div className="market-card-footer">
        <small className="text-muted">{market.demoDisclaimer}</small>
        {onSelectTransaction && market.marketCode === 'AE' && (
          <button className="button button-quiet small" onClick={() => onSelectTransaction('TXN100004')}>
            Inspect Flagged Case (TXN100004) <Icon name="arrow" size={13} />
          </button>
        )}
        {onSelectTransaction && market.marketCode === 'SA' && (
          <button className="button button-quiet small" onClick={() => onSelectTransaction('TXN-SA-1002')}>
            Inspect Flagged Case (TXN-SA-1002) <Icon name="arrow" size={13} />
          </button>
        )}
      </div>
    </div>
  )
}
