import { useState } from 'react'
import type { CorrectiveActionResponse, CreateActionRequest } from '../types/api'
import {
  createCorrectiveAction,
  approveCorrectiveAction,
  updateActionStatus,
  addActionNote,
  updateActionOutcome,
} from '../api/commandCenterApi'
import { Icon } from './Icon'
import { Panel, SectionHeading, StatusBadge } from './UI'

interface CorrectiveActionsViewProps {
  actions: CorrectiveActionResponse[]
  loading: boolean
  onRefresh: () => void
  onSelectTransaction: (id: string) => void
}

export function CorrectiveActionsView({
  actions,
  loading,
  onRefresh,
  onSelectTransaction,
}: CorrectiveActionsViewProps) {
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [approvalModalAction, setApprovalModalAction] = useState<CorrectiveActionResponse | null>(null)
  const [noteModalAction, setNoteModalAction] = useState<CorrectiveActionResponse | null>(null)
  const [outcomeModalAction, setOutcomeModalAction] = useState<CorrectiveActionResponse | null>(null)

  // Form states
  const [newTitle, setNewTitle] = useState('')
  const [newTxnId, setNewTxnId] = useState('TXN100004')
  const [newDesc, setNewDesc] = useState('')
  const [newType, setNewType] = useState('BENEFICIARY_DATA_REMEDIATION')
  const [newTeam, setNewTeam] = useState('Payments Operations')
  const [newRole, setNewRole] = useState('Senior Settlement Officer')
  const [newAssignee, setNewAssignee] = useState('Fahad Al-Maktoum')
  const [newTargetMetric, setNewTargetMetric] = useState('Beneficiary Mismatch Rate')
  const [newBaselineMetric, setNewBaselineMetric] = useState('14.2% Mismatch Failure Rate')

  // Approval form
  const [approvalDecision, setApprovalDecision] = useState<'APPROVED' | 'REJECTED'>('APPROVED')
  const [approvalReviewer, setApprovalReviewer] = useState('VP Transaction Banking')
  const [approvalNotes, setApprovalNotes] = useState('')

  // Note form
  const [noteAuthor, setNoteAuthor] = useState('Operations Analyst')
  const [noteRole, setNoteRole] = useState('L2 Operations Specialist')
  const [noteContent, setNoteContent] = useState('')

  // Outcome form
  const [postMetric, setPostMetric] = useState('')
  const [outcomeStatus, setOutcomeStatus] = useState<'MEASURED_IMPROVEMENT' | 'NO_MATERIAL_CHANGE' | 'INCONCLUSIVE'>('MEASURED_IMPROVEMENT')
  const [outcomeNotes, setOutcomeNotes] = useState('')

  const [saving, setSaving] = useState(false)
  const [errorMsg, setErrorMsg] = useState('')

  const handleCreateAction = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!newTitle.trim() || !newTxnId.trim()) return
    setSaving(true)
    setErrorMsg('')
    try {
      const payload: CreateActionRequest = {
        transactionId: newTxnId.trim(),
        title: newTitle.trim(),
        description: newDesc.trim() || 'Remediation action created from command center.',
        actionType: newType,
        assignedTeam: newTeam,
        assignedRole: newRole,
        assignedTo: newAssignee,
        humanApprovalRequired: true,
        targetMetric: newTargetMetric,
        baselineMetricValue: newBaselineMetric,
      }
      await createCorrectiveAction(payload)
      setShowCreateModal(false)
      setNewTitle('')
      setNewDesc('')
      onRefresh()
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to create action.')
    } finally {
      setSaving(false)
    }
  }

  const handleApproval = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!approvalModalAction) return
    setSaving(true)
    setErrorMsg('')
    try {
      await approveCorrectiveAction(approvalModalAction.id, {
        decision: approvalDecision,
        reviewer: approvalReviewer.trim(),
        notes: approvalNotes.trim() || 'Executive approval recorded via Command Center.',
      })
      setApprovalModalAction(null)
      onRefresh()
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to record approval.')
    } finally {
      setSaving(false)
    }
  }

  const handleAddNote = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!noteModalAction || !noteContent.trim()) return
    setSaving(true)
    setErrorMsg('')
    try {
      await addActionNote(noteModalAction.id, {
        author: noteAuthor.trim(),
        role: noteRole.trim(),
        note: noteContent.trim(),
      })
      setNoteModalAction(null)
      setNoteContent('')
      onRefresh()
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to add note.')
    } finally {
      setSaving(false)
    }
  }

  const handleCompleteStatus = async (actionId: string) => {
    setSaving(true)
    try {
      await updateActionStatus(actionId, { status: 'COMPLETED' })
      onRefresh()
    } catch (err: any) {
      alert(err.message || 'Failed to update status.')
    } finally {
      setSaving(false)
    }
  }

  const handleUpdateOutcome = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!outcomeModalAction || !postMetric.trim()) return
    setSaving(true)
    setErrorMsg('')
    try {
      await updateActionOutcome(outcomeModalAction.id, {
        postActionMetricValue: postMetric.trim(),
        outcomeStatus,
        outcomeNotes: outcomeNotes.trim() || 'Empirical indicator improvement validated post-action completion.',
      })
      setOutcomeModalAction(null)
      setPostMetric('')
      setOutcomeNotes('')
      onRefresh()
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to update outcome.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="page corrective-actions-page">
      <div className="page-title-row">
        <div>
          <div className="eyebrow">CAPABILITY G / CLOSED-LOOP REMEDIATION</div>
          <h1>Corrective Action & Outcome Tracking</h1>
          <p>
            Persisted governance workflow connecting payment exceptions to responsible teams, human approval, and empirical outcome measurement.
          </p>
        </div>
        <div className="title-actions">
          <button className="button button-outline" onClick={onRefresh}>
            <Icon name="refresh" size={15} /> Refresh
          </button>
          <button className="button button-primary" onClick={() => setShowCreateModal(true)}>
            <Icon name="spark" size={15} /> Create Action
          </button>
        </div>
      </div>

      {/* Action Pipeline Overview */}
      <div className="action-pipeline-row">
        <div className="pipeline-column">
          <span className="pipe-count">{actions.filter(a => a.status === 'PROPOSED').length}</span>
          <span className="pipe-label">PROPOSED (Awaiting Approval)</span>
        </div>
        <span className="pipe-arrow"><Icon name="arrow" size={15} /></span>
        <div className="pipeline-column">
          <span className="pipe-count">{actions.filter(a => a.status === 'IN_PROGRESS').length}</span>
          <span className="pipe-label">IN PROGRESS (Assigned)</span>
        </div>
        <span className="pipe-arrow"><Icon name="arrow" size={15} /></span>
        <div className="pipeline-column">
          <span className="pipe-count">{actions.filter(a => a.status === 'COMPLETED').length}</span>
          <span className="pipe-label">COMPLETED & OUTCOME MEASURED</span>
        </div>
      </div>

      {/* Actions Table */}
      <Panel className="actions-table-panel">
        <SectionHeading
          eyebrow="ACTION REGISTRY"
          title={`Active Corrective Actions (${actions.length})`}
          detail="All records are persisted in PostgreSQL with append-only audit histories."
        />
        {loading ? (
          <div className="loading-view small-loading">
            <span className="loader" />
            <span>Loading persisted action history...</span>
          </div>
        ) : actions.length === 0 ? (
          <p className="text-muted">No corrective actions logged yet. Click 'Create Action' to initiate one.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Action Title & Type</th>
                  <th>Transaction</th>
                  <th>Status</th>
                  <th>Assigned Team / Role</th>
                  <th>Approval State</th>
                  <th>Target Metric (Baseline vs Post)</th>
                  <th>Outcome Status</th>
                  <th>Manage</th>
                </tr>
              </thead>
              <tbody>
                {actions.map(action => (
                  <tr key={action.id}>
                    <td>
                      <strong>{action.title}</strong>
                      <small className="d-block text-muted">{action.actionType.replace(/_/g, ' ')}</small>
                    </td>
                    <td>
                      <button
                        className="button button-link mono"
                        onClick={() => onSelectTransaction(action.transactionId)}
                      >
                        {action.transactionId}
                      </button>
                    </td>
                    <td>
                      <StatusBadge status={action.status} />
                    </td>
                    <td>
                      <span>{action.assignedTeam}</span>
                      <small className="d-block text-muted">{action.assignedRole} ({action.assignedTo || 'Unassigned'})</small>
                    </td>
                    <td>
                      <span className={`approval-pill app-${action.approvalStatus.toLowerCase()}`}>
                        {action.approvalStatus}
                      </span>
                      {action.approvedBy && <small className="d-block text-muted">by {action.approvedBy}</small>}
                    </td>
                    <td>
                      <div className="metric-compare-cell">
                        <span>Base: <b>{action.baselineMetricValue || 'N/A'}</b></span>
                        <span>Post: <b className="text-success">{action.postActionMetricValue || 'Pending'}</b></span>
                      </div>
                    </td>
                    <td>
                      <span className={`outcome-chip out-${action.outcomeStatus.toLowerCase()}`}>
                        {action.outcomeStatus.replace(/_/g, ' ')}
                      </span>
                    </td>
                    <td>
                      <div className="action-row-buttons">
                        {action.approvalStatus === 'PENDING' && (
                          <button
                            className="button button-primary small"
                            onClick={() => setApprovalModalAction(action)}
                            title="Review and approve action"
                          >
                            Approve
                          </button>
                        )}
                        {action.status === 'IN_PROGRESS' && (
                          <button
                            className="button button-outline small"
                            onClick={() => handleCompleteStatus(action.id)}
                            title="Mark completed"
                          >
                            Complete
                          </button>
                        )}
                        {action.status === 'COMPLETED' && (
                          <button
                            className="button button-quiet small"
                            onClick={() => setOutcomeModalAction(action)}
                            title="Record outcome measurement"
                          >
                            Outcome
                          </button>
                        )}
                        <button
                          className="button button-quiet small"
                          onClick={() => setNoteModalAction(action)}
                          title="Add audit note"
                        >
                          Note ({action.notes.length})
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Panel>

      {/* Modal: Create Action */}
      {showCreateModal && (
        <div className="modal-backdrop" onClick={() => setShowCreateModal(false)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <div className="modal-head">
              <h3>Create Persisted Corrective Action</h3>
              <button className="icon-button" onClick={() => setShowCreateModal(false)}><Icon name="close" size={17} /></button>
            </div>
            <form onSubmit={handleCreateAction}>
              <label>
                Action Title:
                <input value={newTitle} onChange={e => setNewTitle(e.target.value)} required placeholder="e.g. Beneficiary Whitelist Remediation" />
              </label>
              <label>
                Target Transaction ID:
                <input value={newTxnId} onChange={e => setNewTxnId(e.target.value)} required placeholder="TXN100004" className="mono" />
              </label>
              <label>
                Action Type:
                <select value={newType} onChange={e => setNewType(e.target.value)}>
                  <option value="BENEFICIARY_DATA_REMEDIATION">Beneficiary Data Remediation</option>
                  <option value="CREDIT_LIMIT_REVIEW">Credit Limit & Intraday Liquidity Review</option>
                  <option value="CHANNEL_REPAIR">Payment Clearing Channel Repair</option>
                  <option value="RULE_TUNING">Deterministic Validation Rule Tuning</option>
                  <option value="CUSTOMER_OUTREACH">Corporate Customer Callback & Outreach</option>
                </select>
              </label>
              <div className="input-row-2">
                <label>
                  Assigned Team:
                  <input value={newTeam} onChange={e => setNewTeam(e.target.value)} required />
                </label>
                <label>
                  Assigned Role:
                  <input value={newRole} onChange={e => setNewRole(e.target.value)} required />
                </label>
              </div>
              <label>
                Assignee Name:
                <input value={newAssignee} onChange={e => setNewAssignee(e.target.value)} placeholder="e.g. Fahad Al-Maktoum" />
              </label>
              <div className="input-row-2">
                <label>
                  Target Operational Metric:
                  <input value={newTargetMetric} onChange={e => setNewTargetMetric(e.target.value)} />
                </label>
                <label>
                  Baseline Metric Value:
                  <input value={newBaselineMetric} onChange={e => setNewBaselineMetric(e.target.value)} />
                </label>
              </div>
              <label>
                Action Description & Justification:
                <textarea value={newDesc} onChange={e => setNewDesc(e.target.value)} placeholder="Explain the root cause and planned operational steps..." rows={3} />
              </label>
              {errorMsg && <div className="form-error"><Icon name="alert" size={15} /> {errorMsg}</div>}
              <div className="modal-actions">
                <button type="button" className="button button-quiet" onClick={() => setShowCreateModal(false)}>Cancel</button>
                <button type="submit" className="button button-primary" disabled={saving}>
                  {saving ? 'Creating...' : 'Create Corrective Action'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Approval */}
      {approvalModalAction && (
        <div className="modal-backdrop" onClick={() => setApprovalModalAction(null)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <div className="modal-head">
              <h3>Human Executive Approval Required</h3>
              <button className="icon-button" onClick={() => setApprovalModalAction(null)}><Icon name="close" size={17} /></button>
            </div>
            <p>Review and record formal decision for <strong>{approvalModalAction.title}</strong>.</p>
            <form onSubmit={handleApproval}>
              <div className="approval-toggle-row">
                <button
                  type="button"
                  className={`toggle-btn ${approvalDecision === 'APPROVED' ? 'active-approve' : ''}`}
                  onClick={() => setApprovalDecision('APPROVED')}
                >
                  <Icon name="check" size={15} /> Approve Action
                </button>
                <button
                  type="button"
                  className={`toggle-btn ${approvalDecision === 'REJECTED' ? 'active-reject' : ''}`}
                  onClick={() => setApprovalDecision('REJECTED')}
                >
                  <Icon name="close" size={15} /> Reject Action
                </button>
              </div>
              <label>
                Approver Name & Title:
                <input value={approvalReviewer} onChange={e => setApprovalReviewer(e.target.value)} required />
              </label>
              <label>
                Approval Audit Notes:
                <textarea
                  value={approvalNotes}
                  onChange={e => setApprovalNotes(e.target.value)}
                  placeholder="Record verification rationale or dual-control callback evidence..."
                  rows={3}
                />
              </label>
              {errorMsg && <div className="form-error"><Icon name="alert" size={15} /> {errorMsg}</div>}
              <div className="modal-actions">
                <button type="button" className="button button-quiet" onClick={() => setApprovalModalAction(null)}>Cancel</button>
                <button type="submit" className="button button-primary" disabled={saving}>
                  {saving ? 'Saving...' : 'Record Human Decision'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Note */}
      {noteModalAction && (
        <div className="modal-backdrop" onClick={() => setNoteModalAction(null)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <div className="modal-head">
              <h3>Append Action Audit Note</h3>
              <button className="icon-button" onClick={() => setNoteModalAction(null)}><Icon name="close" size={17} /></button>
            </div>
            <form onSubmit={handleAddNote}>
              <label>
                Author:
                <input value={noteAuthor} onChange={e => setNoteAuthor(e.target.value)} required />
              </label>
              <label>
                Role:
                <input value={noteRole} onChange={e => setNoteRole(e.target.value)} required />
              </label>
              <label>
                Note Content:
                <textarea value={noteContent} onChange={e => setNoteContent(e.target.value)} required rows={3} placeholder="Add operational findings..." />
              </label>
              {noteModalAction.notes.length > 0 && (
                <div className="existing-notes-list">
                  <strong>Existing Audit Log:</strong>
                  {noteModalAction.notes.map(n => (
                    <div key={n.id} className="existing-note-item">
                      <small><strong>{n.author}</strong> ({n.role}) - {new Date(n.createdAt).toLocaleTimeString()}</small>
                      <p>{n.note}</p>
                    </div>
                  ))}
                </div>
              )}
              {errorMsg && <div className="form-error"><Icon name="alert" size={15} /> {errorMsg}</div>}
              <div className="modal-actions">
                <button type="button" className="button button-quiet" onClick={() => setNoteModalAction(null)}>Cancel</button>
                <button type="submit" className="button button-primary" disabled={saving}>
                  {saving ? 'Saving...' : 'Append Note'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Outcome Measurement */}
      {outcomeModalAction && (
        <div className="modal-backdrop" onClick={() => setOutcomeModalAction(null)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <div className="modal-head">
              <h3>Measure Empirical Outcome</h3>
              <button className="icon-button" onClick={() => setOutcomeModalAction(null)}><Icon name="close" size={17} /></button>
            </div>
            <p>
              Compare operational metric before vs after remediation for <strong>{outcomeModalAction.title}</strong>.
            </p>
            <form onSubmit={handleUpdateOutcome}>
              <div className="metric-compare-preview">
                <div><span>Baseline Value:</span> <strong>{outcomeModalAction.baselineMetricValue || 'N/A'}</strong></div>
                <div><Icon name="arrow" size={15} /></div>
                <div><span>Target Metric:</span> <strong>{outcomeModalAction.targetMetric || 'SLA Indicator'}</strong></div>
              </div>
              <label>
                Observed Post-Action Value:
                <input
                  value={postMetric}
                  onChange={e => setPostMetric(e.target.value)}
                  required
                  placeholder="e.g. 0.8% Mismatch Failure Rate"
                />
              </label>
              <label>
                Outcome Assessment:
                <select value={outcomeStatus} onChange={e => setOutcomeStatus(e.target.value as any)}>
                  <option value="MEASURED_IMPROVEMENT">Measured Improvement (Empirically Validated)</option>
                  <option value="NO_MATERIAL_CHANGE">No Material Change Observed</option>
                  <option value="INCONCLUSIVE">Inconclusive (Requires Longer Observation Window)</option>
                </select>
              </label>
              <label>
                Outcome Analysis Notes:
                <textarea
                  value={outcomeNotes}
                  onChange={e => setOutcomeNotes(e.target.value)}
                  placeholder="Document consecutive settlement metrics or reason for evaluation..."
                  rows={3}
                />
              </label>
              {errorMsg && <div className="form-error"><Icon name="alert" size={15} /> {errorMsg}</div>}
              <div className="modal-actions">
                <button type="button" className="button button-quiet" onClick={() => setOutcomeModalAction(null)}>Cancel</button>
                <button type="submit" className="button button-primary" disabled={saving}>
                  {saving ? 'Saving...' : 'Save Outcome Measurement'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
