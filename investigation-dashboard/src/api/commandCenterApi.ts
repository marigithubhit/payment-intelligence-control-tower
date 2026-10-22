import { apiRequest } from './client'
import type {
  ExecutiveOverview,
  RegionalIntelligence,
  ExceptionPattern,
  PriorityQueueItem,
  ExecutiveDecisionBrief,
  CorrectiveActionResponse,
  CreateActionRequest,
  ActionApprovalRequest,
  ActionStatusUpdateRequest,
  ActionNoteRequest,
  ActionOutcomeUpdateRequest,
} from '../types/api'

export async function getExecutiveOverview(): Promise<ExecutiveOverview> {
  return apiRequest<ExecutiveOverview>('/api/v1/command-center/overview')
}

export async function getRegionalIntelligence(): Promise<RegionalIntelligence> {
  return apiRequest<RegionalIntelligence>('/api/v1/command-center/regional')
}

export async function getExceptionPatterns(): Promise<ExceptionPattern[]> {
  return apiRequest<ExceptionPattern[]>('/api/v1/command-center/exception-patterns')
}

export async function getPriorityQueue(): Promise<PriorityQueueItem[]> {
  return apiRequest<PriorityQueueItem[]>('/api/v1/command-center/priority-queue')
}

export async function getExecutiveDecisionBrief(transactionId: string): Promise<ExecutiveDecisionBrief> {
  return apiRequest<ExecutiveDecisionBrief>(`/api/v1/command-center/decision-brief/${encodeURIComponent(transactionId)}`)
}

export async function getAllActions(): Promise<CorrectiveActionResponse[]> {
  return apiRequest<CorrectiveActionResponse[]>('/api/v1/command-center/actions')
}

export async function getActionsForTransaction(transactionId: string): Promise<CorrectiveActionResponse[]> {
  return apiRequest<CorrectiveActionResponse[]>(`/api/v1/command-center/actions/transaction/${encodeURIComponent(transactionId)}`)
}

export async function createCorrectiveAction(payload: CreateActionRequest): Promise<CorrectiveActionResponse> {
  return apiRequest<CorrectiveActionResponse>('/api/v1/command-center/actions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export async function approveCorrectiveAction(actionId: string, payload: ActionApprovalRequest): Promise<CorrectiveActionResponse> {
  return apiRequest<CorrectiveActionResponse>(`/api/v1/command-center/actions/${encodeURIComponent(actionId)}/approval`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export async function updateActionStatus(actionId: string, payload: ActionStatusUpdateRequest): Promise<CorrectiveActionResponse> {
  return apiRequest<CorrectiveActionResponse>(`/api/v1/command-center/actions/${encodeURIComponent(actionId)}/status`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export async function addActionNote(actionId: string, payload: ActionNoteRequest): Promise<CorrectiveActionResponse> {
  return apiRequest<CorrectiveActionResponse>(`/api/v1/command-center/actions/${encodeURIComponent(actionId)}/notes`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export async function updateActionOutcome(actionId: string, payload: ActionOutcomeUpdateRequest): Promise<CorrectiveActionResponse> {
  return apiRequest<CorrectiveActionResponse>(`/api/v1/command-center/actions/${encodeURIComponent(actionId)}/outcome`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}
