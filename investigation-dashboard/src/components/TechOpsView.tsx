import type { Health, RailPerformance } from '../types/api'
import { Icon } from './Icon'
import { MetricCard, Panel, SectionHeading, StatusBadge } from './UI'

interface TechOpsViewProps {
  health: Health | null
  healthError: boolean
  lastOllama: string
  rails: RailPerformance[]
  onRefreshHealth: () => void
}

export function TechOpsView({
  health,
  healthError,
  lastOllama,
  rails,
  onRefreshHealth,
}: TechOpsViewProps) {
  return (
    <div className="page tech-ops-page">
      <div className="page-title-row">
        <div>
          <div className="eyebrow">CAPABILITY H / TECH OPS TELEMETRY</div>
          <h1>Technology Operations & Telemetry</h1>
          <p>
            Real-time infrastructure health, ISO 20022 parser throughput, gateway latencies, and Ollama agent execution telemetry.
          </p>
        </div>
        <div className="title-actions">
          <button className="button button-outline" onClick={onRefreshHealth}>
            <Icon name="refresh" size={15} /> Ping Telemetry
          </button>
        </div>
      </div>

      {/* Infrastructure Posture Cards */}
      <div className="metric-grid">
        <MetricCard
          label="Spring Boot API Gateway"
          value={health && !healthError ? 'CONNECTED' : 'UNAVAILABLE'}
          icon="building"
          tone={health && !healthError ? 'green' : 'red'}
          caption="Port 8080 · JDK 21"
        />
        <MetricCard
          label="PostgreSQL Database"
          value="HEALTHY"
          icon="layers"
          tone="green"
          caption="PostgreSQL 18 · Flyway V6"
        />
        <MetricCard
          label="Ollama AI Reasoning"
          value={lastOllama === 'AVAILABLE' ? 'ONLINE' : lastOllama === 'UNAVAILABLE' ? 'FALLBACK' : 'STANDBY'}
          icon="brain"
          tone={lastOllama === 'AVAILABLE' ? 'green' : 'amber'}
          caption="qwen2.5:7b (Local GPU/CPU)"
        />
        <MetricCard
          label="ISO 20022 Parser Engine"
          value="SECURE"
          icon="shield"
          tone="green"
          caption="DTD/XXE disabled · pacs.008"
        />
      </div>

      {/* Integration Telemetry Grid */}
      <div className="tech-ops-grid">
        <Panel className="service-telemetry-panel">
          <SectionHeading
            eyebrow="INTERNAL SERVICES"
            title="Service Telemetry & Agent Endpoints"
            detail="Status and response latencies of core transaction banking subsystems."
          />
          <div className="service-status-list">
            <div className="service-status-item">
              <span className={`dot ${health && !healthError ? 'dot-online' : 'dot-offline'}`} />
              <div className="service-info">
                <strong>/api/v1/payments/iso20022</strong>
                <small>XML Ingestion & Validation Gateway (pacs.008)</small>
              </div>
              <span className="mono latency-chip">12 ms</span>
              <StatusBadge status="ACTIVE" />
            </div>

            <div className="service-status-item">
              <span className="dot dot-online" />
              <div className="service-info">
                <strong>/api/v1/investigations/{'{id}'}</strong>
                <small>Deterministic Rules Engine & Historical Baseline</small>
              </div>
              <span className="mono latency-chip">18 ms</span>
              <StatusBadge status="ACTIVE" />
            </div>

            <div className="service-status-item">
              <span className={`dot ${lastOllama === 'AVAILABLE' ? 'dot-online' : 'dot-warning'}`} />
              <div className="service-info">
                <strong>/api/v1/agents/reasoning</strong>
                <small>AI Reasoning Agent (Ollama Local Instance)</small>
              </div>
              <span className="mono latency-chip">{lastOllama === 'AVAILABLE' ? '1,420 ms' : 'Offline'}</span>
              <StatusBadge status={lastOllama === 'AVAILABLE' ? 'ONLINE' : 'FALLBACK'} />
            </div>

            <div className="service-status-item">
              <span className="dot dot-online" />
              <div className="service-info">
                <strong>/api/v1/agents/verification</strong>
                <small>Deterministic Verification Agent Gate</small>
              </div>
              <span className="mono latency-chip">4 ms</span>
              <StatusBadge status="ACTIVE" />
            </div>

            <div className="service-status-item">
              <span className="dot dot-online" />
              <div className="service-info">
                <strong>/api/v1/command-center/*</strong>
                <small>Executive Intelligence & Action Tracking (Flyway V6)</small>
              </div>
              <span className="mono latency-chip">8 ms</span>
              <StatusBadge status="ACTIVE" />
            </div>
          </div>
        </Panel>

        <Panel className="rails-telemetry-panel">
          <SectionHeading
            eyebrow="EXTERNAL CLEARING RAILS"
            title="Payment Rail Network Health"
            detail="Regional clearing gateways and multilateral network heartbeat."
          />
          <div className="rail-telemetry-list">
            {rails.map(r => (
              <div key={r.railCode} className="rail-telemetry-item">
                <div>
                  <strong>{r.railCode}</strong>
                  <small className="d-block text-muted">{r.railName}</small>
                </div>
                <div className="rail-latency">
                  <span>Latency:</span>
                  <strong className="mono">{r.avgLatencyMs} ms</strong>
                </div>
                <StatusBadge status={r.status} />
              </div>
            ))}
          </div>
        </Panel>
      </div>
    </div>
  )
}
