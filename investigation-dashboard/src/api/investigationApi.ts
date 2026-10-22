import { apiRequest } from './client'
import type { HistoricalAnalysis, InvestigationCase, InvestigationResult, InvestigationReview, ReviewDecision } from '../types/api'
export const getInvestigation = (transactionId: string) => apiRequest<InvestigationResult>(`/api/v1/investigations/${encodeURIComponent(transactionId)}`)
export const getInvestigationCase = (caseId: string) => apiRequest<InvestigationCase>(`/api/v1/investigations/cases/${encodeURIComponent(caseId)}`)
export const getHistory = (transactionId: string) => apiRequest<HistoricalAnalysis>(`/api/v1/investigations/${encodeURIComponent(transactionId)}/history`)
export const getReviewHistory = (caseId: string) => apiRequest<InvestigationReview[]>(`/api/v1/investigations/cases/${encodeURIComponent(caseId)}/reviews`)
export const submitReview = (caseId: string, review: { decision: ReviewDecision; reason: string; reviewer: string }) => apiRequest<InvestigationReview>(`/api/v1/investigations/cases/${encodeURIComponent(caseId)}/review`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(review) })
