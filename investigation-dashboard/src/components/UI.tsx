import type { ReactNode, ComponentProps } from 'react'
import { Icon } from './Icon'

export function SectionHeading({ eyebrow, title, detail, action }: { eyebrow?: string; title: string; detail?: string; action?: ReactNode }) {
  return <div className="section-heading"><div>{eyebrow && <div className="eyebrow">{eyebrow}</div>}<h2>{title}</h2>{detail && <p>{detail}</p>}</div>{action}</div>
}
export function Panel({ children, className = '', id }: { children: ReactNode; className?: string; id?: string }) { return <section className={`panel ${className}`} id={id}>{children}</section> }
export function RiskBadge({ level }: { level?: string | null }) { const value = (level || 'UNKNOWN').toLowerCase(); return <span className={`risk-badge risk-${value}`}>{level || 'Unavailable'}</span> }
export function StatusBadge({ status }: { status?: string | null }) { const value = (status || 'UNKNOWN').toLowerCase().replaceAll('_', '-'); return <span className={`status-badge status-${value}`}><span className="status-dot" />{status || 'Unavailable'}</span> }
export function EmptyState({ title, detail, icon = 'file' }: { title: string; detail: string; icon?: 'file' | 'search' }) { return <div className="empty-state"><span className="empty-icon"><Icon name={icon} size={20} /></span><strong>{title}</strong><p>{detail}</p></div> }
export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) { return <div className="error-state"><Icon name="alert" size={20}/><div><strong>Unable to load this view</strong><p>{message}</p>{onRetry && <button className="text-button" onClick={onRetry}>Try again <Icon name="arrow" size={14}/></button>}</div></div> }
export function DetailField({ label, value, mono = false, copy = false }: { label: string; value: string | number | null | undefined; mono?: boolean; copy?: boolean }) {
  const text = value == null || value === '' ? 'Not exposed by this API' : String(value)
  return <div className="detail-field"><span className="field-label">{label}</span><span className={`field-value ${mono ? 'mono' : ''}`} title={text}>{text}{copy && value && <button className="copy-button" aria-label={`Copy ${label}`} onClick={() => navigator.clipboard?.writeText(String(value))}>Copy</button>}</span></div>
}
export function MetricCard({ label, value, icon, caption, tone = '' }: { label: string; value: string; icon: ComponentProps<typeof Icon>['name']; caption: string; tone?: string }) {
  return <article className="metric-card"><span className={`metric-icon ${tone}`}><Icon name={icon}/></span><div className="metric-label">{label}</div><strong>{value}</strong><div className="metric-caption">{caption}</div></article>
}
