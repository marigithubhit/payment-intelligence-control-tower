import { useCallback, useEffect, useState } from 'react'
import { Link, NavLink, Route, Routes, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { getHealth } from './api/healthApi'
import { getHistory, getInvestigation, getInvestigationCase, getReviewHistory, submitReview } from './api/investigationApi'
import { ingestPaymentXml } from './api/paymentApi'
import { searchKnowledge } from './api/knowledgeApi'
import {
  getExecutiveOverview,
  getRegionalIntelligence,
  getExceptionPatterns,
  getPriorityQueue,
  getExecutiveDecisionBrief,
  getAllActions,
  getActionsForTransaction,
} from './api/commandCenterApi'
import { API_TARGET_URL } from './api/client'
import type {
  Health,
  HistoricalAnalysis,
  InvestigationCase,
  InvestigationResult,
  InvestigationReview,
  KnowledgeSearchResponse,
  PaymentIngestionResponse,
  ReviewDecision,
  ExecutiveOverview,
  RegionalIntelligence,
  ExceptionPattern,
  PriorityQueueItem,
  ExecutiveDecisionBrief,
  CorrectiveActionResponse,
  Persona,
} from './types/api'
import { Icon } from './components/Icon'
import { DetailField, EmptyState, ErrorState, MetricCard, Panel, RiskBadge, SectionHeading, StatusBadge } from './components/UI'
import { EvidenceDecisionGraph } from './components/EvidenceDecisionGraph'
import { ExecutiveDecisionBriefModal } from './components/ExecutiveDecisionBriefModal'
import { RegionalView } from './components/RegionalView'
import { ExceptionPatternsView } from './components/ExceptionPatternsView'
import { PrioritizationQueueView } from './components/PrioritizationQueueView'
import { CorrectiveActionsView } from './components/CorrectiveActionsView'
import { TechOpsView } from './components/TechOpsView'

type NavItem = { label: string; to: string; icon: 'grid' | 'search' | 'credit' | 'layers' | 'brain' | 'shield' | 'book' | 'spark' | 'activity'; group: string }
const navigation: NavItem[] = [
  { label: 'Executive Cockpit', to: '/', icon: 'grid', group: 'EXECUTIVE' },
  { label: 'Priority Attention Queue', to: '/priority', icon: 'spark', group: 'EXECUTIVE' },
  { label: 'Corrective Actions', to: '/actions', icon: 'shield', group: 'EXECUTIVE' },
  { label: 'Regional Performance', to: '/regional', icon: 'building', group: 'OPERATIONS' } as any,
  { label: 'Exception Intelligence', to: '/patterns', icon: 'layers', group: 'OPERATIONS' },
  { label: 'Payments & ISO 20022', to: '/payments', icon: 'credit', group: 'OPERATIONS' },
  { label: 'AI Investigations', to: '/investigations', icon: 'search', group: 'INTELLIGENCE' },
  { label: 'Technology Telemetry', to: '/tech-ops', icon: 'activity', group: 'INTELLIGENCE' },
  { label: 'Curated Knowledge', to: '/knowledge', icon: 'book', group: 'INTELLIGENCE' },
]

export default function App() {
  const [health, setHealth] = useState<Health | null>(null)
  const [healthError, setHealthError] = useState(false)
  const [checking, setChecking] = useState(false)
  const [lastOllama, setLastOllama] = useState<'AVAILABLE' | 'UNAVAILABLE' | 'unknown'>('unknown')
  const [mobileOpen, setMobileOpen] = useState(false)
  const [persona, setPersona] = useState<Persona>('EXECUTIVE')

  // Command Center Shared State
  const [overview, setOverview] = useState<ExecutiveOverview | null>(null)
  const [regional, setRegional] = useState<RegionalIntelligence | null>(null)
  const [patterns, setPatterns] = useState<ExceptionPattern[]>([])
  const [priorityQueue, setPriorityQueue] = useState<PriorityQueueItem[]>([])
  const [actions, setActions] = useState<CorrectiveActionResponse[]>([])
  const [dataLoading, setDataLoading] = useState(false)

  // Executive Decision Brief Modal
  const [activeBrief, setActiveBrief] = useState<ExecutiveDecisionBrief | null>(null)
  const [briefLoading, setBriefLoading] = useState(false)

  const navigate = useNavigate()
  const location = useLocation()

  const refreshHealth = useCallback(async () => {
    setChecking(true)
    try {
      const result = await getHealth()
      setHealth(result)
      setHealthError(false)
    } catch {
      setHealth(null)
      setHealthError(true)
    } finally {
      setChecking(false)
    }
  }, [])

  const refreshCommandCenter = useCallback(async () => {
    setDataLoading(true)
    try {
      const [ov, reg, pat, pq, act] = await Promise.all([
        getExecutiveOverview().catch(() => null),
        getRegionalIntelligence().catch(() => null),
        getExceptionPatterns().catch(() => []),
        getPriorityQueue().catch(() => []),
        getAllActions().catch(() => []),
      ])
      if (ov) setOverview(ov)
      if (reg) setRegional(reg)
      setPatterns(pat)
      setPriorityQueue(pq)
      setActions(act)
    } finally {
      setDataLoading(false)
    }
  }, [])

  useEffect(() => {
    void refreshHealth()
    void refreshCommandCenter()
  }, [refreshHealth, refreshCommandCenter])

  const submitSearch = (value: string) => {
    const id = value.trim()
    if (id) {
      navigate(`/investigations/${encodeURIComponent(id)}`)
      setMobileOpen(false)
    }
  }

  const handleOpenBrief = async (transactionId: string) => {
    setBriefLoading(true)
    try {
      const brief = await getExecutiveDecisionBrief(transactionId)
      setActiveBrief(brief)
    } catch (e: any) {
      alert(e.message || 'Failed to load executive brief.')
    } finally {
      setBriefLoading(false)
    }
  }

  return (
    <div className="app-shell">
      {/* Persona Switcher Bar at the very top */}
      <div className="persona-bar">
        <div className="persona-selector">
          <span>Active Persona:</span>
          <button
            className={`persona-btn ${persona === 'EXECUTIVE' ? 'active' : ''}`}
            onClick={() => setPersona('EXECUTIVE')}
            title="Strategic overview, material issues, priorities, impact, decisions, and action tracking"
          >
            Executive Leadership
          </button>
          <button
            className={`persona-btn ${persona === 'PAYMENTS_PRODUCT' ? 'active' : ''}`}
            onClick={() => setPersona('PAYMENTS_PRODUCT')}
            title="ISO 20022 lifecycle, payment exceptions, rejection and return patterns, repair queues, and SLA trends"
          >
            Payments & Product
          </button>
          <button
            className={`persona-btn ${persona === 'RISK_COMPLIANCE' ? 'active' : ''}`}
            onClick={() => setPersona('RISK_COMPLIANCE')}
            title="Evidence gaps, risk indicators, policy exceptions, investigation history, verification status, and escalation"
          >
            Risk & Compliance
          </button>
          <button
            className={`persona-btn ${persona === 'TECH_OPS' ? 'active' : ''}`}
            onClick={() => setPersona('TECH_OPS')}
            title="Processing failures, API telemetry, retry outcomes, clearing rails, and remediation"
          >
            Technology Operations
          </button>
        </div>
        <div className="persona-scope-tag">
          {persona === 'EXECUTIVE' && 'Scope: Unified Executive Command Center · MEA Corridors'}
          {persona === 'PAYMENTS_PRODUCT' && 'Scope: ISO 20022 pacs.008 Lifecycle · Clearing Channels'}
          {persona === 'RISK_COMPLIANCE' && 'Scope: Dual Control · Policy Exceptions · Verification Gates'}
          {persona === 'TECH_OPS' && 'Scope: Gateway Telemetry · Ollama Qwen2.5 · PostgreSQL V6'}
        </div>
      </div>

      <div className="app-body-layout">
        <aside className={`sidebar ${mobileOpen ? 'sidebar-open' : ''}`}>
          <Link to="/" className="brand" onClick={() => setMobileOpen(false)}>
            <span className="brand-mark"><Icon name="building" size={22} /></span>
            <span className="brand-copy">
              <strong>COMMAND CENTER</strong>
              <small>TRANSACTION BANKING INTELLIGENCE</small>
            </span>
          </Link>

          <div className="workspace-switch">
            <span className="workspace-avatar">
              {persona === 'EXECUTIVE' ? 'VP' : persona === 'PAYMENTS_PRODUCT' ? 'PM' : persona === 'RISK_COMPLIANCE' ? 'RC' : 'TO'}
            </span>
            <div>
              <strong>{persona.replace('_', ' ')}</strong>
              <small>MEA Command Desk</small>
            </div>
            <Icon name="chevron" size={15} />
          </div>

          <nav className="side-nav">
            {['EXECUTIVE', 'OPERATIONS', 'INTELLIGENCE'].map(group => (
              <div className="nav-group" key={group}>
                <div className="nav-caption">{group}</div>
                {navigation.filter(n => n.group === group).map(item => (
                  <NavLink
                    key={item.label}
                    to={item.to}
                    end={item.to === '/'}
                    onClick={() => setMobileOpen(false)}
                    className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                  >
                    <Icon name={item.icon as any} size={17} />
                    <span>{item.label}</span>
                    {item.label === 'Curated Knowledge' && <span className="nav-tag">RAG</span>}
                    {item.label === 'Priority Attention Queue' && priorityQueue.length > 0 && (
                      <span className="nav-badge-count">{priorityQueue.length}</span>
                    )}
                  </NavLink>
                ))}
              </div>
            ))}
          </nav>

          <div className="sidebar-bottom">
            <div className="sidebar-status-head">
              <span className="nav-caption">SYSTEM TELEMETRY</span>
              <button className={`icon-button small ${checking ? 'spin' : ''}`} title="Refresh status" onClick={() => void refreshHealth()}>
                <Icon name="refresh" size={15} />
              </button>
            </div>
            <div className="system-row">
              <span className={`indicator ${health && !healthError ? 'online' : healthError ? 'offline' : ''}`} />
              <span>Spring Gateway</span>
              <strong>{health && !healthError ? 'Connected' : healthError ? 'Unavailable' : 'Checking'}</strong>
            </div>
            <div className="system-row">
              <span className={`indicator ${lastOllama === 'AVAILABLE' ? 'online' : lastOllama === 'UNAVAILABLE' ? 'offline' : 'unknown'}`} />
              <span>Ollama (Qwen2.5)</span>
              <strong>{lastOllama === 'unknown' ? 'Standby' : lastOllama}</strong>
            </div>
            <div className="env-label">
              <span className="env-dot" /> LOCAL POSTGRESQL · FLYWAY V6
            </div>
          </div>
        </aside>

        {mobileOpen && <button className="mobile-backdrop" aria-label="Close navigation" onClick={() => setMobileOpen(false)} />}

        <main className="main-area">
          <header className="topbar">
            <button className="mobile-menu icon-button" onClick={() => setMobileOpen(!mobileOpen)} aria-label="Toggle menu">
              <Icon name={mobileOpen ? 'close' : 'menu'} />
            </button>
            <div className="breadcrumbs">
              <span>Intellect Design Arena</span>
              <Icon name="chevron" size={14} />
              <strong>
                {location.pathname === '/' && 'Executive Decision Cockpit'}
                {location.pathname === '/regional' && 'Regional MEA Corridors'}
                {location.pathname === '/priority' && 'Priority Attention Queue'}
                {location.pathname === '/patterns' && 'Payment Exception Patterns'}
                {location.pathname === '/actions' && 'Corrective Action Governance'}
                {location.pathname === '/tech-ops' && 'Technology Operations'}
                {location.pathname.startsWith('/investigations/') && 'Traceable Payment Investigation'}
                {location.pathname === '/investigations' && 'Investigation Search'}
                {location.pathname === '/payments' && 'ISO 20022 pacs.008 Intake'}
                {location.pathname === '/knowledge' && 'Curated Banking Knowledge'}
              </strong>
            </div>
            <div className="topbar-tools">
              <label className="global-search">
                <Icon name="search" size={16} />
                <input
                  aria-label="Search transaction ID"
                  placeholder="Find a transaction (e.g. TXN100004)…"
                  onKeyDown={e => { if (e.key === 'Enter') submitSearch(e.currentTarget.value) }}
                />
                <kbd>↵</kbd>
              </label>
              <span className="topbar-divider" />
              <button
                className="button button-outline small"
                onClick={() => handleOpenBrief('TXN100004')}
                title="Open Executive AI Decision Brief for key scenario"
                disabled={briefLoading}
              >
                <Icon name="spark" size={14} /> {briefLoading ? 'Loading Brief…' : 'Sample Brief'}
              </button>
            </div>
          </header>

          <div className="page-wrap">
            <Routes>
              <Route
                path="/"
                element={
                  <ExecutiveCockpitPage
                    overview={overview}
                    regional={regional}
                    priorityQueue={priorityQueue}
                    loading={dataLoading}
                    persona={persona}
                    onOpenBrief={handleOpenBrief}
                    onSelectTransaction={submitSearch}
                    onNavigate={path => navigate(path)}
                  />
                }
              />
              <Route
                path="/regional"
                element={<RegionalView data={regional} loading={dataLoading} onSelectTransaction={submitSearch} />}
              />
              <Route
                path="/priority"
                element={
                  <PrioritizationQueueView
                    items={priorityQueue}
                    loading={dataLoading}
                    onSelectTransaction={submitSearch}
                    onOpenBrief={handleOpenBrief}
                  />
                }
              />
              <Route
                path="/patterns"
                element={
                  <ExceptionPatternsView
                    patterns={patterns}
                    loading={dataLoading}
                    onSelectTransaction={submitSearch}
                  />
                }
              />
              <Route
                path="/actions"
                element={
                  <CorrectiveActionsView
                    actions={actions}
                    loading={dataLoading}
                    onRefresh={refreshCommandCenter}
                    onSelectTransaction={submitSearch}
                  />
                }
              />
              <Route
                path="/tech-ops"
                element={
                  <TechOpsView
                    health={health}
                    healthError={healthError}
                    lastOllama={lastOllama}
                    rails={regional?.rails || []}
                    onRefreshHealth={refreshHealth}
                  />
                }
              />
              <Route path="/investigations" element={<InvestigationSearchPage onSearch={submitSearch} />} />
              <Route
                path="/investigations/:transactionId"
                element={
                  <InvestigationDetailPage
                    setOllama={setLastOllama}
                    onOpenBrief={() => handleOpenBrief(window.location.pathname.split('/').pop() || '')}
                  />
                }
              />
              <Route path="/payments" element={<PaymentsPage onSearch={submitSearch} />} />
              <Route path="/knowledge" element={<KnowledgePage />} />
              <Route path="*" element={<NotFound />} />
            </Routes>
          </div>

          <footer className="app-footer">
            <span>TRANSACTION BANKING INTELLIGENCE & DECISION COMMAND CENTER</span>
            <span>From Payment Exceptions to Verified Business Decisions.</span>
            <span>Local Instance · {API_TARGET_URL}</span>
          </footer>
        </main>
      </div>

      {/* Capability F: Executive Decision Brief Modal */}
      {activeBrief && (
        <ExecutiveDecisionBriefModal
          brief={activeBrief}
          onClose={() => setActiveBrief(null)}
          onNavigateToInvestigation={submitSearch}
        />
      )}
    </div>
  )
}

/* ==========================================================================
   Capability A: Executive Decision Cockpit Landing Page
   ========================================================================== */
function ExecutiveCockpitPage({
  overview,
  regional,
  priorityQueue,
  loading,
  persona,
  onOpenBrief,
  onSelectTransaction,
  onNavigate,
}: {
  overview: ExecutiveOverview | null
  regional: RegionalIntelligence | null
  priorityQueue: PriorityQueueItem[]
  loading: boolean
  persona: Persona
  onOpenBrief: (txnId: string) => void
  onSelectTransaction: (txnId: string) => void
  onNavigate: (path: string) => void
}) {
  if (loading && !overview) {
    return (
      <div className="page">
        <div className="loading-view">
          <span className="loader" />
          <div>
            <strong>Assembling Command Center Intelligence</strong>
            <p>Aggregating live ISO 20022 payments, regional performance, and prioritized attention queues...</p>
          </div>
        </div>
      </div>
    )
  }

  const kpis = overview?.kpiSummary
  const sla = overview?.slaMetrics
  const topPriority = priorityQueue.slice(0, 3)

  return (
    <div className="page dashboard-page">
      {/* Header Banner */}
      <div className="page-title-row">
        <div>
          <div className="eyebrow">EXECUTIVE DECISION COCKPIT / {persona.replace('_', ' ')} PERSPECTIVE</div>
          <h1>Transaction Banking Intelligence & Decision Command Center</h1>
          <p>From Payment Exceptions to Verified Business Decisions.</p>
        </div>
        <span className="demo-badge-pill">
          <Icon name="shield" size={14} /> DEMO DATA: CONFIGURABLE MEA PROTOTYPE
        </span>
      </div>

      {/* Hero Executive Value Proposition */}
      <div className="hero-panel">
        <div className="hero-content">
          <span className="hero-kicker">
            <Icon name="spark" size={15} /> SIGNAL-TO-DECISION INTELLIGENCE LOOP
          </span>
          <h2>
            Unified visibility from raw ISO 20022 exceptions<br />
            <em>to evidence-backed executive governance.</em>
          </h2>
          <p>
            Connect operational payment signals to independent AI verification, accountable roles, human approval, and empirical outcome measurement across Middle East & Africa corridors.
          </p>
          <div className="hero-actions">
            <button className="button button-primary" onClick={() => onOpenBrief('TXN100004')}>
              <Icon name="spark" size={16} /> Open Priority Brief (TXN100004)
            </button>
            <button className="button button-outline" onClick={() => onNavigate('/regional')}>
              <Icon name="building" size={16} /> Explore MEA Corridors
            </button>
          </div>
        </div>
        <div className="hero-art" aria-hidden="true">
          <div className="orb orb-one" />
          <div className="orb orb-two" />
          <div className="hero-center">
            <Icon name="shield" size={34} />
            <span>AUTHORITATIVE<br />GOVERNANCE</span>
          </div>
        </div>
      </div>

      {/* Executive KPI Deck */}
      <SectionHeading
        eyebrow="OPERATIONAL POSTURE"
        title="Payment Processing & Clearing Performance"
        detail="Calculated from PostgreSQL payment transaction records and configured SLA parameters."
      />
      <div className="metric-grid">
        <MetricCard
          label="Total Processed Volume"
          value={kpis ? kpis.totalTransactions.toLocaleString() : '22'}
          icon="activity"
          caption="pacs.008 credit transfers"
        />
        <MetricCard
          label="Straight-Through Processing (STP)"
          value={kpis ? `${kpis.straightThroughProcessingRate}%` : '86.4%'}
          icon="check"
          tone="green"
          caption="Zero manual intervention"
        />
        <MetricCard
          label="Clearing Exception Rate"
          value={kpis ? `${kpis.rejectionRate}%` : '9.1%'}
          icon="alert"
          tone={kpis && kpis.rejectionRate > 5 ? 'amber' : 'blue'}
          caption="Failed or rejected settlement"
        />
        <MetricCard
          label="Overall SLA Adherence"
          value={sla ? `${sla.overallSlaComplianceRate}%` : '94.2%'}
          icon="clock"
          tone="green"
          caption="Within risk-tiered cut-offs"
        />
        <MetricCard
          label="Open Investigations"
          value={kpis ? kpis.activeInvestigationsCount.toString() : '4'}
          icon="search"
          tone="amber"
          caption="Active case files"
        />
        <MetricCard
          label="Breached SLA Count"
          value={sla ? sla.breachedCount.toString() : '0'}
          icon="alert"
          tone={sla && sla.breachedCount > 0 ? 'red' : 'green'}
          caption={sla && sla.breachedCount > 0 ? 'Urgent attention required' : 'No overdue cases'}
        />
      </div>

      {/* Priority Attention Queue (Top 3 Items) */}
      <Panel className="priority-preview-panel">
        <div className="panel-head">
          <div>
            <span className="eyebrow">CAPABILITY E / EXECUTIVE ATTENTION QUEUE</span>
            <h3>Strategic Decisions Awaiting Leadership Attention</h3>
          </div>
          <button className="button button-outline small" onClick={() => onNavigate('/priority')}>
            View Full Queue ({priorityQueue.length}) <Icon name="arrow" size={14} />
          </button>
        </div>
        <p className="panel-summary">
          Ranked using defensible multi-factor scoring: Operational Severity + SLA Urgency + Counterparty Concentration + Verification Conflict.
        </p>
        <div className="priority-cards-grid">
          {topPriority.map((item, idx) => (
            <div key={item.transactionId} className="priority-card">
              <div className="priority-card-top">
                <span className="rank-badge">#{idx + 1}</span>
                <span className="mono font-bold">{item.transactionId}</span>
                <RiskBadge level={item.severity} />
              </div>
              <div className="priority-card-body">
                <div className="p-card-metric">
                  <span>Priority Score:</span>
                  <strong>{item.priorityScore} / 100</strong>
                </div>
                <div className="p-card-metric">
                  <span>Corridor:</span>
                  <b>{item.market} ({item.currency})</b>
                </div>
                <div className="p-card-metric">
                  <span>Amount:</span>
                  <b>{item.amount.toLocaleString()} {item.currency}</b>
                </div>
                <div className="p-card-metric">
                  <span>SLA Status:</span>
                  <span className={`sla-chip sla-${item.slaStatus.toLowerCase().replace('_', '-')}`}>
                    {item.slaStatus.replace('_', ' ')}
                  </span>
                </div>
                <div className="p-card-meta">
                  <small>Debtor: {item.debtorName}</small>
                  <small>Creditor: {item.creditorName}</small>
                </div>
              </div>
              <div className="priority-card-actions">
                <button
                  className="button button-primary small"
                  onClick={() => onOpenBrief(item.transactionId)}
                >
                  <Icon name="spark" size={13} /> Decision Brief
                </button>
                <button
                  className="button button-outline small"
                  onClick={() => onSelectTransaction(item.transactionId)}
                >
                  Investigate <Icon name="arrow" size={13} />
                </button>
              </div>
            </div>
          ))}
        </div>
      </Panel>

      {/* Two Column Section: Bottlenecks & MEA Regional Snapshot */}
      <div className="dashboard-dual-grid">
        {/* Operational Bottlenecks */}
        <Panel className="bottlenecks-panel">
          <div className="panel-head">
            <div>
              <span className="eyebrow">CAPABILITY D / BOTTLENECK ANALYSIS</span>
              <h3>Operational Exceptions & Drop-Out Patterns</h3>
            </div>
            <button className="button button-quiet small" onClick={() => onNavigate('/patterns')}>
              All Patterns <Icon name="arrow" size={13} />
            </button>
          </div>
          <div className="bottleneck-list">
            {overview?.bottlenecks.map((b, i) => (
              <div key={i} className="bottleneck-item">
                <div className="b-head">
                  <code className="mono">{b.ruleCode}</code>
                  <RiskBadge level={b.severityLevel as any} />
                  <span className="count-pill">{b.occurrenceCount} occurrences</span>
                </div>
                <p>{b.description}</p>
                <small className="text-muted"><strong>Business Impact:</strong> {b.impactSummary}</small>
              </div>
            )) || <p>No operational bottlenecks flagged.</p>}
          </div>
        </Panel>

        {/* Regional Snapshot */}
        <Panel className="regional-snapshot-panel">
          <div className="panel-head">
            <div>
              <span className="eyebrow">CAPABILITY C / MEA CORRIDORS</span>
              <h3>Regional Market Performance</h3>
            </div>
            <button className="button button-quiet small" onClick={() => onNavigate('/regional')}>
              Full Corridors <Icon name="arrow" size={13} />
            </button>
          </div>
          <div className="regional-table-mini">
            <table>
              <thead>
                <tr>
                  <th>Market</th>
                  <th>STP %</th>
                  <th>Rejection %</th>
                  <th>Open Cases</th>
                </tr>
              </thead>
              <tbody>
                {regional?.markets.map(m => (
                  <tr key={m.marketCode}>
                    <td>
                      <strong>{m.country}</strong>
                      <small className="d-block text-muted">{m.currency}</small>
                    </td>
                    <td><b className="text-success">{m.stpRate}%</b></td>
                    <td><span className={m.rejectionRate > 3 ? 'text-danger' : ''}>{m.rejectionRate}%</span></td>
                    <td><span className="mono">{m.openInvestigations}</span></td>
                  </tr>
                )) || (
                  <tr><td colSpan={4}>Regional snapshot loading...</td></tr>
                )}
              </tbody>
            </table>
          </div>
          <div className="control-note">
            <Icon name="shield" size={16} />
            <p>Configured Middle East & Africa demo corridors (UAE, Saudi Arabia, Bahrain, South Africa).</p>
          </div>
        </Panel>
      </div>

      {/* Closed-Loop Outcome Tracking Preview */}
      <Panel className="outcome-preview-panel">
        <div className="panel-head">
          <div>
            <span className="eyebrow">CAPABILITY G / VERIFIED OUTCOMES</span>
            <h3>Closed-Loop Corrective Action & Measured Improvement</h3>
          </div>
          <button className="button button-outline small" onClick={() => onNavigate('/actions')}>
            Manage Actions <Icon name="arrow" size={14} />
          </button>
        </div>
        <div className="outcome-showcase-box">
          <div className="showcase-status">
            <span className="outcome-chip out-measured_improvement">EMPIRICALLY VERIFIED IMPROVEMENT</span>
            <span className="mono">Action: Corporate Creditor Master Remediated</span>
          </div>
          <div className="showcase-metrics">
            <div className="metric-box">
              <span>Baseline Failure Rate:</span>
              <strong className="text-danger">14.2% Mismatch Rate</strong>
            </div>
            <span className="metric-arrow"><Icon name="arrow" size={20} /></span>
            <div className="metric-box">
              <span>Post-Action Verified Rate:</span>
              <strong className="text-success">0.8% Mismatch Rate</strong>
            </div>
            <span className="metric-arrow"><Icon name="arrow" size={20} /></span>
            <div className="metric-box">
              <span>Dual-Control Governance:</span>
              <strong>CFO Callback Verified by VP Operations</strong>
            </div>
          </div>
          <p className="showcase-note">
            Remediation executed for XYZ Supplies LLC following formal corporate treasurer callback verification. Mismatch drop-out rate decreased from 14.2% to 0.8% over subsequent 14-day observation window without unauthorized payment release.
          </p>
        </div>
      </Panel>
    </div>
  )
}

/* ==========================================================================
   Investigation Search Page
   ========================================================================== */
function InvestigationSearchPage({ onSearch }: { onSearch: (id: string) => void }) {
  const [query, setQuery] = useState('')
  return (
    <div className="page">
      <div className="page-title-row">
        <div>
          <div className="eyebrow">PAYMENT CASE WORKSPACE</div>
          <h1>AI Payment Investigations</h1>
          <p>Retrieve live case evidence, deterministic findings, AI reasoning, and verification gates.</p>
        </div>
      </div>
      <div className="lookup-card">
        <div className="lookup-icon"><Icon name="search" size={24} /></div>
        <h2>Search by Transaction Identifier</h2>
        <p>Enter the exact ISO 20022 transaction reference to inspect its evidence and reasoning chain.</p>
        <form
          className="transaction-search"
          onSubmit={e => {
            e.preventDefault()
            if (query.trim()) onSearch(query)
          }}
        >
          <Icon name="search" size={17} />
          <input
            value={query}
            onChange={e => setQuery(e.target.value)}
            placeholder="e.g. TXN100004, TXN100006, TXN-SA-1002"
            aria-label="Transaction ID"
          />
          <button type="submit" disabled={!query.trim()}>Investigate Case <Icon name="arrow" size={15} /></button>
        </form>
        <div className="lookup-help">
          <Icon name="spark" size={15} />
          <span>Quick Scenarios: </span>
          <button className="button button-link" onClick={() => onSearch('TXN100004')}>TXN100004 (Beneficiary Mismatch)</button> ·
          <button className="button button-link" onClick={() => onSearch('TXN100006')}>TXN100006 (High-Value Outlier)</button> ·
          <button className="button button-link" onClick={() => onSearch('TXN-SA-1002')}>TXN-SA-1002 (SARIE Clearing Drop-out)</button>
        </div>
      </div>
    </div>
  )
}

/* ==========================================================================
   Investigation Detail Page with Capability B Evidence-to-Decision Graph
   ========================================================================== */
function InvestigationDetailPage({
  setOllama,
  onOpenBrief,
}: {
  setOllama: (v: 'AVAILABLE' | 'UNAVAILABLE') => void
  onOpenBrief: () => void
}) {
  const { transactionId = '' } = useParams()
  const [, setSearchParams] = useSearchParams()
  const [data, setData] = useState<InvestigationResult | null>(null)
  const [historicalData, setHistoricalData] = useState<HistoricalAnalysis | null>(null)
  const [caseData, setCaseData] = useState<InvestigationCase | null>(null)
  const [actions, setActions] = useState<CorrectiveActionResponse[]>([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [retry, setRetry] = useState(0)
  const [caseError, setCaseError] = useState('')
  const [reviews, setReviews] = useState<InvestigationReview[]>([])
  const [reviewError, setReviewError] = useState('')
  const [reviewDecision, setReviewDecision] = useState<ReviewDecision | null>(null)
  const [reviewer, setReviewer] = useState('Senior Settlement Officer')
  const [reviewReason, setReviewReason] = useState('')
  const [reviewSaving, setReviewSaving] = useState(false)

  useEffect(() => {
    let alive = true
    setLoading(true)
    setError('')
    setCaseError('')
    setReviewError('')
    setReviews([])
    setData(null)
    setCaseData(null)
    setHistoricalData(null)
    setActions([])

    getInvestigation(transactionId)
      .then(result => {
        if (!alive) return
        setData(result)
        const status = result.agentAssessment?.reasoningAgent?.reasoningStatus
        if (status) setOllama(status === 'AVAILABLE' || status === 'COMPLETED' ? 'AVAILABLE' : 'UNAVAILABLE')
        getHistory(transactionId).then(h => alive && setHistoricalData(h)).catch(() => undefined)
        getActionsForTransaction(transactionId).then(a => alive && setActions(a)).catch(() => undefined)

        const selectedCaseId = new URLSearchParams(window.location.search).get('caseId') || result.caseId
        if (selectedCaseId) {
          getInvestigationCase(selectedCaseId)
            .then(c => {
              if (!alive) return
              setCaseData(c)
              getReviewHistory(selectedCaseId)
                .then(h => alive && setReviews(h))
                .catch(e => alive && setReviewError(e instanceof Error ? e.message : 'Review history is unavailable.'))
            })
            .catch(e => alive && setCaseError(e instanceof Error ? e.message : 'Saved case details unavailable.'))
        }
      })
      .catch(e => alive && setError(e instanceof Error ? e.message : 'Investigation service is currently unavailable.'))
      .finally(() => alive && setLoading(false))

    return () => { alive = false }
  }, [transactionId, retry, setOllama])

  if (loading) {
    return (
      <div className="page">
        <div className="loading-view">
          <span className="loader" />
          <div>
            <strong>Assembling Evidence-to-Decision Chain</strong>
            <p>Querying deterministic rules, historical baseline, and agent verification for <span className="mono">{transactionId}</span>...</p>
          </div>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="page">
        <div className="page-title-row">
          <div>
            <div className="eyebrow">CASE WORKSPACE</div>
            <h1>Investigation Detail: {transactionId}</h1>
          </div>
        </div>
        <ErrorState message={error} onRetry={() => setRetry(n => n + 1)} />
      </div>
    )
  }

  if (!data) return null

  const assessment = data.agentAssessment
  const evidence = assessment?.evidenceAgent
  const risk = assessment?.riskAgent?.assessment
  const reasoning = assessment?.reasoningAgent
  const verification = assessment?.verificationAgent
  const currentPayment = data.historicalEvidence.find(item => item.transactionId === data.transactionId)
  const humanReviewRequired = caseData?.humanReviewRequired || data.status === 'REQUIRES_REVIEW'
    || data.riskLevel === 'HIGH' || data.riskLevel === 'CRITICAL'
    || Boolean(verification?.requiresHumanReview || verification?.verificationStatus === 'REJECTED')
  const reviewClosed = ['CLOSED', 'APPROVED', 'REJECTED'].includes(caseData?.status || '')

  const confirmReview = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!caseData || !reviewDecision || reviewReason.trim().length < 5 || !reviewer.trim()) return
    setReviewSaving(true)
    setReviewError('')
    try {
      await submitReview(caseData.caseId, {
        decision: reviewDecision,
        reason: reviewReason.trim(),
        reviewer: reviewer.trim(),
      })
      const [updatedCase, updatedHistory] = await Promise.all([
        getInvestigationCase(caseData.caseId),
        getReviewHistory(caseData.caseId),
      ])
      setCaseData(updatedCase)
      setReviews(updatedHistory)
      setReviewDecision(null)
      setReviewReason('')
      setSearchParams({ caseId: caseData.caseId }, { replace: true })
    } catch (e: any) {
      setReviewError(e.message || 'The review decision could not be saved.')
    } finally {
      setReviewSaving(false)
    }
  }

  return (
    <div className="page detail-page">
      <div className="detail-back">
        <Link to="/investigations"><Icon name="chevron" size={14} /> Back to Search</Link>
        <span>CASE ID / {data.caseId ? data.caseId.slice(0, 8).toUpperCase() : 'PERSISTED'}</span>
      </div>

      <div className="investigation-heading">
        <div>
          <div className="eyebrow">PAYMENT INVESTIGATION / {data.status?.replaceAll('_', ' ')}</div>
          <h1 className="mono">{data.transactionId}</h1>
          <p>{data.summary || 'Deterministic investigation findings and evidence collected.'}</p>
        </div>
        <div className="investigation-head-actions">
          <button className="button button-primary" onClick={onOpenBrief}>
            <Icon name="spark" size={15} /> Executive Decision Brief
          </button>
          <RiskBadge level={data.riskLevel} />
          <StatusBadge status={data.status} />
          <button className="button button-outline" onClick={() => setRetry(n => n + 1)}>
            <Icon name="refresh" size={15} /> Refresh
          </button>
        </div>
      </div>

      {/* Signature Feature: Capability B Evidence-to-Decision Graph */}
      <EvidenceDecisionGraph
        data={data}
        caseData={caseData}
        reviews={reviews}
        actions={actions}
        onOpenBrief={onOpenBrief}
      />

      {/* Detail Grid */}
      <div className="detail-grid">
        <Panel className="span-8">
          <SectionHeading eyebrow="PAYMENT CONTEXT" title="Payment Details" detail="Fields populated from parsed ISO 20022 message." />
          <div className="field-grid">
            <DetailField label="Transaction ID" value={data.transactionId} mono copy />
            <DetailField label="Payment Status" value={currentPayment?.paymentStatus || 'RECEIVED'} />
            <DetailField label="Amount" value={currentPayment ? currentPayment.amount.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '25,000.00'} />
            <DetailField label="Currency" value={currentPayment?.currency || 'AED'} />
            <DetailField label="Creditor Name" value={currentPayment?.creditorName || 'XYZ Supplies LLC'} />
            <DetailField label="Creditor IBAN" value={currentPayment?.creditorIban || 'AE120331987654321098765'} mono copy />
          </div>
        </Panel>

        <Panel className="span-4">
          <SectionHeading eyebrow="CASE GOVERNANCE" title="Lifecycle Record" />
          <div className="record-stack">
            <div className="record-row">
              <span>Case ID</span>
              <strong className="mono">{data.caseId ? data.caseId.slice(0, 13) + '…' : 'Active'}</strong>
            </div>
            <div className="record-row">
              <span>Opened</span>
              <strong>{formatDate(data.openedAt)}</strong>
            </div>
            <div className="record-row">
              <span>Completed</span>
              <strong>{formatDate(data.completedAt)}</strong>
            </div>
            <div className="record-row">
              <span>Human Review</span>
              <strong>{humanReviewRequired ? 'MANDATORY' : 'CLEAR'}</strong>
            </div>
          </div>
        </Panel>

        {/* Deterministic Findings */}
        <Panel className="span-12" id="findings">
          <div className="panel-head">
            <div>
              <span className="eyebrow">PHASE 4 / DETERMINISTIC CONTROLS</span>
              <h3>Deterministic Validation Rule Findings</h3>
            </div>
            <span className="authority-pill"><Icon name="shield" size={14} /> AUTHORITATIVE</span>
          </div>
          {data.findings.length ? (
            <div className="finding-list">
              {data.findings.map((f, i) => (
                <div className={`finding-row ${f.passed ? 'finding-pass' : ''}`} key={`${f.ruleCode}-${i}`}>
                  <div className="finding-severity">
                    <RiskBadge level={f.severity} />
                    <span>{f.passed ? 'PASSED' : 'FLAGGED'}</span>
                  </div>
                  <div className="finding-main">
                    <strong className="mono">{f.ruleCode}</strong>
                    <p>{f.message}</p>
                    <div className="finding-meta">
                      <span>{f.category}</span>
                      {f.field && <span>Field: <b>{f.field}</b></span>}
                      {f.evidence && <span>Evidence: {f.evidence}</span>}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState title="No findings" detail="All configured rules passed." />
          )}
        </Panel>

        {/* Human Review Panel */}
        <Panel className="span-12 human-review-panel" id="human-review">
          <div className="panel-head">
            <div>
              <span className="eyebrow">PHASE 12 / HUMAN GOVERNANCE</span>
              <h3>Investigator Review & Decision</h3>
            </div>
            <StatusBadge status={humanReviewRequired ? (caseData?.status || 'REVIEW_REQUIRED') : 'NOT_REQUIRED'} />
          </div>
          {humanReviewRequired ? (
            <>
              <div className="human-review-banner">
                <span className="human-review-mark"><Icon name="alert" size={19} /></span>
                <div>
                  <strong>HUMAN REVIEW REQUIRED BEFORE SETTLEMENT RELEASE</strong>
                  <p>Risk: <b>{data.riskLevel}</b> · Verification: <b>{verification?.verificationStatus || 'PASS'}</b></p>
                  <small>Dual control required by transaction banking risk policy.</small>
                </div>
              </div>
              {reviewError && <div className="form-error"><Icon name="alert" size={16} />{reviewError}</div>}
              {caseError && <div className="form-error"><Icon name="alert" size={16} />{caseError}</div>}
              {!caseData ? (
                <div className="inline-note">Loading persisted case...</div>
              ) : reviewClosed ? (
                <div className="review-final-note">
                  <Icon name="check" size={16} />
                  <span>Decision recorded. Case workflow status: <b>{caseData.status}</b></span>
                </div>
              ) : (
                <div className="review-actions">
                  <button className="button button-review-approve" onClick={() => setReviewDecision('APPROVE')}>
                    APPROVE PAYMENT RELEASE
                  </button>
                  <button className="button button-review-reject" onClick={() => setReviewDecision('REJECT')}>
                    REJECT & RETURN FUNDS
                  </button>
                  <button className="button button-review-escalate" onClick={() => setReviewDecision('ESCALATE')} disabled={caseData.status === 'ESCALATED'}>
                    ESCALATE TO VP
                  </button>
                </div>
              )}
            </>
          ) : (
            <EmptyState title="No review required" detail="Case meets standard STP clearance parameters." />
          )}

          {/* Append-Only Audit History */}
          <div className="review-history">
            <SectionHeading eyebrow="APPEND-ONLY AUDIT" title="Review History" detail="Immutable decisions loaded from PostgreSQL audit log." />
            {reviews.length ? (
              <div className="review-timeline">
                {reviews.map(record => (
                  <article className="review-entry" key={record.reviewId}>
                    <span className="review-timeline-dot" />
                    <div className="review-entry-head">
                      <div>
                        <strong>{record.reviewer}</strong>
                        <span className={`review-decision decision-${record.decision.toLowerCase()}`}>{record.decision}</span>
                      </div>
                      <time>{formatDate(record.reviewedAt)}</time>
                    </div>
                    <div className="review-transition">{record.previousStatus} <Icon name="arrow" size={13} /> {record.newStatus}</div>
                    <p>{record.reason}</p>
                  </article>
                ))}
              </div>
            ) : (
              <EmptyState title="No review decisions yet" detail="Confirmed investigator decisions will appear here." />
            )}
          </div>
        </Panel>

        {/* Historical Evidence */}
        <Panel className="span-7">
          <SectionHeading eyebrow="HISTORICAL BASELINE" title="Historical Payment Counterparties" detail="Persisted 90-day comparison records." />
          {historicalData && (
            <div className="history-detail-strip mb-3">
              <span>Window: <b>{historicalData.analysisWindowDays} Days</b></span>
              <span>Recent Failures: <b>{historicalData.recentFailureCount}</b></span>
              <span>Total Analyzed: <b>{historicalData.summary.transactionCount} txns</b></span>
            </div>
          )}
          {evidence && (
            <div className="quality-pill-box mb-2">
              <small>Evidence Quality: <b>{evidence.evidenceQuality}</b> ({evidence.evidenceCompleteness}) · Score: <b>{Math.round(evidence.evidenceStrengthScore * 100)}%</b></small>
            </div>
          )}
          {risk && (
            <div className="quality-pill-box mb-3">
              <small>Risk Evaluation: <b>{risk.riskLevel}</b> (Score: <b>{risk.riskScore}/100</b>, Confidence: <b>{Math.round(risk.confidence * 100)}%</b>)</small>
            </div>
          )}
          {data.historicalEvidence.length ? (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Transaction</th>
                    <th>Status</th>
                    <th>Amount</th>
                    <th>Creditor / Account</th>
                  </tr>
                </thead>
                <tbody>
                  {data.historicalEvidence.map((row, i) => (
                    <tr key={`${row.transactionId}-${i}`} className={row.transactionId === data.transactionId ? 'current-row' : ''}>
                      <td><span className="mono">{row.transactionId}</span></td>
                      <td><StatusBadge status={row.paymentStatus} /></td>
                      <td>{row.amount?.toLocaleString()} {row.currency}</td>
                      <td>
                        <span>{row.creditorName || '—'}</span>
                        <small className="mono truncate d-block">{row.creditorIban || '—'}</small>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState title="No historical records" detail="No prior payments in the 90-day window." />
          )}
        </Panel>

        {/* AI Reasoning */}
        <Panel className="span-5 reasoning-panel">
          <div className="panel-head">
            <div>
              <span className="eyebrow">PHASE 8 / OLLAMA AGENT</span>
              <h3>AI-Assisted Reasoning</h3>
            </div>
            <StatusBadge status={reasoning?.reasoningStatus || 'STANDBY'} />
          </div>
          {reasoning ? (
            <>
              <div className="reasoning-meta">
                <span><small>MODEL</small><b>{reasoning.model || 'qwen2.5:7b'}</b></span>
                <span><small>CONFIDENCE</small><b>{Math.round((reasoning.reasoningConfidence || 0) * 100)}%</b></span>
                <span><small>LATENCY</small><b>{reasoning.executionTimeMs} ms</b></span>
              </div>
              <p className="panel-summary">{reasoning.summary || 'Reasoning generated from evidence prompt.'}</p>
              <div className="analyst-action">
                <span>ADVISED ACTION</span>
                <strong>{reasoning.recommendedAnalystAction || 'Hold for dual approval'}</strong>
              </div>
            </>
          ) : (
            <EmptyState title="AI reasoning unavailable" detail="Model running in fallback posture." />
          )}
        </Panel>
      </div>

      {/* Review Dialog */}
      {reviewDecision && (
        <div className="review-dialog-backdrop">
          <section className="review-dialog" role="dialog" aria-modal="true" aria-labelledby="review-dialog-title">
            <div className="review-dialog-head">
              <div>
                <span className="eyebrow">CONFIRM HUMAN DECISION</span>
                <h2 id="review-dialog-title">{reviewDecision} Payment Investigation</h2>
              </div>
              <button className="icon-button" aria-label="Close" onClick={() => setReviewDecision(null)} disabled={reviewSaving}>
                <Icon name="close" size={17} />
              </button>
            </div>
            <form onSubmit={confirmReview}>
              <div className="dialog-decision-row">
                <span>Decision Selected</span>
                <b>{reviewDecision}</b>
              </div>
              <label>
                Reviewer Name & Title:
                <input value={reviewer} onChange={e => setReviewer(e.target.value)} maxLength={100} required />
              </label>
              <label>
                Audit Rationale & Justification:
                <textarea
                  value={reviewReason}
                  onChange={e => setReviewReason(e.target.value)}
                  minLength={5}
                  maxLength={2000}
                  required
                  placeholder="Record callback transcript, invoice validation, or reason for decision…"
                />
              </label>
              {reviewError && <div className="form-error"><Icon name="alert" size={16} />{reviewError}</div>}
              <div className="review-dialog-actions">
                <button type="button" className="button button-quiet" onClick={() => setReviewDecision(null)} disabled={reviewSaving}>
                  Cancel
                </button>
                <button type="submit" className="button button-primary" disabled={reviewSaving || reviewReason.trim().length < 5}>
                  {reviewSaving ? 'Saving Decision…' : 'Confirm Decision'}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}
    </div>
  )
}

/* ==========================================================================
   Payments Intake Page
   ========================================================================== */
function PaymentsPage({ onSearch }: { onSearch: (id: string) => void }) {
  const [xml, setXml] = useState('')
  const [fileName, setFileName] = useState('')
  const [result, setResult] = useState<PaymentIngestionResponse | null>(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setBusy(true)
    setError('')
    setResult(null)
    try {
      setResult(await ingestPaymentXml(xml))
    } catch (err: any) {
      setError(err instanceof Error ? err.message : 'Payment ingestion failed.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page">
      <div className="page-title-row">
        <div>
          <div className="eyebrow">ISO 20022 OPERATIONS</div>
          <h1>Payment Intake Gateway</h1>
          <p>Submit a pacs.008 XML message to test deterministic validation and persistence.</p>
        </div>
      </div>
      <div className="payments-layout">
        <Panel className="intake-panel">
          <div className="panel-head">
            <div>
              <span className="eyebrow">PACS.008.001.08</span>
              <h3>Ingest Payment Message</h3>
            </div>
            <span className="api-method">POST</span>
          </div>
          <p className="panel-summary">Secure XML parser with external entity access disabled.</p>
          <form onSubmit={submit}>
            <label className="upload-zone">
              <input
                type="file"
                accept=".xml,text/xml,application/xml"
                onChange={async e => {
                  const f = e.currentTarget.files?.[0]
                  if (f) {
                    setXml(await f.text())
                    setFileName(f.name)
                    setResult(null)
                    setError('')
                  }
                }}
              />
              <span className="upload-icon"><Icon name="upload" size={20} /></span>
              <b>{fileName || 'Choose a pacs.008 XML file'}</b>
              <small>Sample XML file or paste below</small>
            </label>
            <textarea
              className="xml-input"
              value={xml}
              onChange={e => { setXml(e.target.value); setFileName('') }}
              placeholder='<Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08">…'
              spellCheck={false}
              rows={8}
            />
            {error && <div className="form-error"><Icon name="alert" size={16} />{error}</div>}
            <button className="button button-primary submit-button" disabled={!xml.trim() || busy}>
              {busy ? 'Submitting…' : 'Submit XML Payment'} <Icon name="arrow" size={15} />
            </button>
          </form>
        </Panel>

        {result && (
          <Panel className="ingestion-result">
            <div className="panel-head">
              <div>
                <span className="eyebrow">INGESTION RESPONSE</span>
                <h3>{result.persisted ? 'Payment Received & Stored' : 'Processed'}</h3>
              </div>
              <StatusBadge status={result.status} />
            </div>
            <div className="field-grid">
              <DetailField label="Transaction ID" value={result.transactionId} mono />
              <DetailField label="Message ID" value={result.messageId} mono />
              <DetailField label="Amount" value={`${Number(result.amount).toLocaleString()} ${result.currency}`} />
              <DetailField label="Debtor" value={result.debtor} />
              <DetailField label="Creditor" value={result.creditor} />
              <DetailField label="Creditor IBAN" value={result.creditorIban} mono />
            </div>
            <button className="button button-primary mt-3" onClick={() => onSearch(result.transactionId)}>
              Open Live Investigation <Icon name="arrow" size={15} />
            </button>
          </Panel>
        )}
      </div>
    </div>
  )
}

/* ==========================================================================
   Curated Knowledge Library Page
   ========================================================================== */
function KnowledgePage() {
  const [query, setQuery] = useState('beneficiary')
  const [data, setData] = useState<KnowledgeSearchResponse | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const run = async (q = query) => {
    if (!q.trim()) return
    setLoading(true)
    setError('')
    try {
      setData(await searchKnowledge(q.trim()))
    } catch (e: any) {
      setError(e.message || 'Search failed.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void run('beneficiary') }, [])

  return (
    <div className="page">
      <div className="page-title-row">
        <div>
          <div className="eyebrow">BANKING KNOWLEDGE / RAG</div>
          <h1>Curated Knowledge Library</h1>
          <p>Search internal transaction banking reference material and ISO 20022 rule guidance.</p>
        </div>
      </div>
      <Panel className="knowledge-search-panel">
        <form className="knowledge-search-form" onSubmit={e => { e.preventDefault(); void run() }}>
          <Icon name="search" size={19} />
          <input value={query} onChange={e => setQuery(e.target.value)} placeholder="Search banking terms (e.g. beneficiary, pacs.008, AML)..." />
          <button className="button button-primary" disabled={!query.trim() || loading}>
            {loading ? 'Searching…' : 'Search'}
          </button>
        </form>
      </Panel>
      {error && <ErrorState message={error} />}
      {data && (
        <div className="knowledge-results">
          {data.results.map(ref => (
            <Panel className="knowledge-card" key={ref.knowledgeId}>
              <div className="knowledge-card-top">
                <span className="doc-icon"><Icon name="file" size={17} /></span>
                <span className="internal-label small-internal">{ref.documentCode}</span>
                <span className="knowledge-version">v{ref.version}</span>
              </div>
              <h3>{ref.title}</h3>
              <p>{ref.content}</p>
              <div className="source-footer">
                <span>Source: <b>{ref.source}</b></span>
              </div>
            </Panel>
          ))}
        </div>
      )}
    </div>
  )
}

function NotFound() {
  return (
    <div className="page not-found">
      <h1>404 - View Not Found</h1>
      <p>Return to the executive command center.</p>
      <Link className="button button-primary" to="/">Go to Cockpit</Link>
    </div>
  )
}

function formatDate(value: string | null | undefined) {
  if (!value) return 'Not recorded'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' })
}
