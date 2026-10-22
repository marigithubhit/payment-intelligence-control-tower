export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export type ValidationFinding = { ruleCode: string; severity: RiskLevel; category: string; message: string; field: string | null; evidence: string | null; passed: boolean }
export type HistoricalPaymentEvidence = { transactionId: string; paymentStatus: string; amount: number; currency: string; creditorName: string | null; creditorIban: string | null; createdAt: string | null }
export type HistoricalAnalysis = {
  transactionId: string; analysisWindowDays: number; analysisWindowStart: string | null; analysisWindowEnd: string | null;
  summary: { transactionCount: number; successfulTransactionCount: number; failedTransactionCount: number; amountSampleCount: number; averageAmount: number | null; minimumAmount: number | null; maximumAmount: number | null; mostRecentTransactionDate: string | null };
  currentAmount: number | null; currency: string | null; amountRatio: number | null;
  historicalBeneficiaries: { creditorIban: string; successfulTransactionCount: number; mostRecentTransactionDate: string | null }[];
  recentFailureCount: number; recentSuccessCount: number; totalRecentTransactions: number; failureRate: number;
  mostRecentFailureDate: string | null; historicalTransactionIds: string[]
}
export type AssessedEvidenceItem = { classification: string; evidenceType: string; ruleCode: string | null; sourceReference: string | null; description: string; observedValue: string | null; expectedValue: string | null }
export type EvidenceAgent = { transactionId: string; evidenceItems: AssessedEvidenceItem[]; evidenceQuality: string; evidenceCompleteness: string; evidenceStrengthScore: number; missingEvidence: string[]; conflictingEvidence: boolean; summary: string; executionDurationMillis: number }
export type RiskFactor = { code: string; severity: RiskLevel; points: number; description: string; evidenceReferences: string[] }
export type RiskAssessment = { transactionId: string; riskScore: number; riskLevel: RiskLevel; confidence: number; riskFactors: RiskFactor[]; recommendedAction: string; requiresHumanReview: boolean; explanation: string }
export type RiskAgent = { transactionId: string; assessment: RiskAssessment; executionDurationMillis: number }
export type ReasoningStatus = 'AVAILABLE' | 'COMPLETED' | 'UNAVAILABLE' | 'REJECTED'
export type ReasoningAgent = {
  transactionId: string; reasoningStatus: ReasoningStatus; summary: string | null;
  observedFacts: { evidenceId: string; fact: string }[];
  evidenceAssessment: { assessment: string; importance: string; evidenceIds: string[] }[];
  reasoning: { statement: string; evidenceIds: string[] }[];
  evidenceReferences: { evidenceId: string; evidenceType: string; ruleCode: string | null; sourceReference: string | null; fact: string }[];
  uncertainties: string[]; recommendedAnalystAction: string | null; reasoningConfidence: number | null;
  confidenceCapped: boolean; model: string | null; promptVersion: string | null; executionTimeMs: number;
  deterministicInvestigationAvailable: boolean; reasoningError: string | null
}
export type VerificationAgent = {
  transactionId: string; verificationStatus: string; verificationScore: number;
  findings: { code: string; severity: string; message: string; evidenceIds: string[] }[];
  verifiedClaims: string[]; unsupportedClaims: string[]; verifiedEvidenceReferences: string[];
  recommendationVerified: boolean; riskLevelPreserved: RiskLevel; requiresHumanReview: boolean
}
export type InvestigationResult = {
  transactionId: string; status: string; riskLevel: RiskLevel; findings: ValidationFinding[];
  historicalEvidence: HistoricalPaymentEvidence[]; recommendation: string; caseId: string | null;
  summary: string; openedAt: string | null; completedAt: string | null;
  agentAssessment: { transactionId: string; evidenceAgent: EvidenceAgent; riskAgent: RiskAgent; reasoningAgent: ReasoningAgent | null; verificationAgent: VerificationAgent | null } | null
}
export type InvestigationCase = { caseId: string; transactionId: string; status: string; riskLevel: RiskLevel; humanReviewRequired: boolean; recommendation: string; summary: string; openedAt: string | null; completedAt: string | null; createdAt: string | null; updatedAt: string | null; evidence: { evidenceType: string; category: string | null; ruleCode: string | null; severity: string | null; description: string; sourceReference: string; observedValue: string | null; expectedValue: string | null; createdAt: string | null }[] }
export type ReviewDecision = 'APPROVE' | 'REJECT' | 'ESCALATE'
export type InvestigationReview = { reviewId: string; caseId: string; transactionId: string; decision: ReviewDecision; reason: string; reviewer: string; previousStatus: string; newStatus: string; reviewedAt: string }
export type Health = { status: string; application: string }
export type PaymentIngestionResponse = { status: string; messageId: string | null; transactionId: string; amount: number; currency: string; debtor: string; creditor: string; debtorIban: string | null; creditorIban: string | null; persisted: boolean; findings: ValidationFinding[] }
export type KnowledgeSearchResponse = { query: string; results: { knowledgeId: string; documentCode: string; title: string; topic: string; content: string; source: string; version: string }[] }
export type ApiError = { timestamp?: string; status?: number; code?: string; message?: string }

// Executive Command Center Types
export type KpiSummary = {
  totalTransactions: number
  totalAmountProcessed: number
  primaryCurrency: string
  straightThroughProcessingRate: number
  rejectionRate: number
  returnRate: number
  exceptionCount: number
  activeInvestigationsCount: number
}

export type InvestigationSummary = {
  totalCases: number
  requiresReviewCount: number
  approvedCount: number
  rejectedCount: number
  escalatedCount: number
  closedCount: number
}

export type RiskBreakdown = {
  criticalCount: number
  highCount: number
  mediumCount: number
  lowCount: number
}

export type SlaMetrics = {
  breachedCount: number
  atRiskCount: number
  onTrackCount: number
  averageResolutionHours: number
  overallSlaComplianceRate: number
}

export type AgeingBuckets = {
  under4Hours: number
  fourTo24Hours: number
  oneToThreeDays: number
  over3Days: number
}

export type OperationalBottleneck = {
  ruleCode: string
  category: string
  description: string
  occurrenceCount: number
  severityLevel: string
  impactSummary: string
}

export type PriorityScoreBreakdown = {
  operationalSeverityPoints: number
  slaUrgencyPoints: number
  volumeConcentrationPoints: number
  verificationConflictPoints: number
  totalScore: number
}

export type PriorityQueueItem = {
  transactionId: string
  caseId: string | null
  market: string
  currency: string
  amount: number
  debtorName: string
  creditorName: string
  severity: RiskLevel
  riskScore: number
  priorityScore: number
  scoreBreakdown: PriorityScoreBreakdown
  slaStatus: 'BREACHED' | 'AT_RISK' | 'ON_TRACK'
  hoursInQueue: number
  recommendedAction: string
  assignedTeam: string
  workflowStatus: string
  verificationStatus: string
}

export type ExecutiveOverview = {
  kpiSummary: KpiSummary
  investigationSummary: InvestigationSummary
  riskBreakdown: RiskBreakdown
  slaMetrics: SlaMetrics
  ageingBuckets: AgeingBuckets
  bottlenecks: OperationalBottleneck[]
  priorityAttentionQueue: PriorityQueueItem[]
  openCorrectiveActionsCount: number
  reportingPeriod: string
  calculatedAt: string
  dataDisclaimer: string
}

export type MarketPerformance = {
  marketCode: string
  marketName: string
  country: string
  currency: string
  activeRails: string[]
  totalVolume: number
  totalValue: number
  stpRate: number
  rejectionRate: number
  returnRate: number
  avgProcessingDelayMinutes: number
  openInvestigations: number
  slaAdherencePercent: number
  operationalStatus: string
  primaryExceptionCategory: string
  demoDisclaimer: string
}

export type RailPerformance = {
  railCode: string
  railName: string
  marketCode: string
  messageType: string
  volume: number
  value: number
  avgLatencyMs: number
  rejectionRate: number
  status: string
}

export type RegionalIntelligence = {
  markets: MarketPerformance[]
  rails: RailPerformance[]
  observationWindow: string
  generatedAt: string
  syntheticDataNotice: string
}

export type ExceptionPattern = {
  patternId: string
  patternName: string
  category: string
  ruleCode: string
  affectedCount: number
  affectedValueTotal: number
  sampleTransactionIds: string[]
  detectionLogic: string
  confidenceLimitations: string
  recommendedNextStep: string
  operationalSeverity: string
  observedMarket: string
}

export type ExecutiveDecisionBrief = {
  transactionId: string
  caseId: string | null
  headline: string
  observedSignal: string
  amount: number
  currency: string
  debtor: string
  creditor: string
  market: string
  supportingEvidence: string[]
  whatEvidenceEstablishes: string
  whatRemainsUncertain: string
  businessImplication: string
  recommendedNextStep: string
  accountableTeam: string
  humanApprovalRequired: boolean
  requiredApprovalRole: string
  verificationStatus: string
  verificationScore: number
  preservedRiskLevel: string
  followUpMeasure: string
  liveReasoningUsed: boolean
  generatedAt: string
}

export type ActionNoteResponse = {
  id: string
  author: string
  role: string
  note: string
  createdAt: string
}

export type CorrectiveActionResponse = {
  id: string
  caseId: string | null
  transactionId: string
  title: string
  description: string
  actionType: string
  status: 'PROPOSED' | 'IN_PROGRESS' | 'COMPLETED' | 'REJECTED' | 'CANCELLED'
  assignedTeam: string
  assignedRole: string
  assignedTo: string | null
  humanApprovalRequired: boolean
  approvalStatus: 'PENDING' | 'APPROVED' | 'REJECTED' | 'NOT_REQUIRED'
  approvedBy: string | null
  approvedAt: string | null
  approvalNotes: string | null
  targetMetric: string | null
  baselineMetricValue: string | null
  postActionMetricValue: string | null
  outcomeStatus: 'PENDING_EVALUATION' | 'MEASURED_IMPROVEMENT' | 'NO_MATERIAL_CHANGE' | 'INCONCLUSIVE'
  outcomeNotes: string | null
  createdAt: string
  updatedAt: string
  completedAt: string | null
  notes: ActionNoteResponse[]
}

export type CreateActionRequest = {
  caseId?: string | null
  transactionId: string
  title: string
  description: string
  actionType: string
  assignedTeam: string
  assignedRole: string
  assignedTo?: string | null
  humanApprovalRequired: boolean
  targetMetric?: string | null
  baselineMetricValue?: string | null
}

export type ActionApprovalRequest = {
  decision: 'APPROVED' | 'REJECTED'
  reviewer: string
  notes?: string
}

export type ActionStatusUpdateRequest = {
  status: 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
}

export type ActionNoteRequest = {
  author: string
  role: string
  note: string
}

export type ActionOutcomeUpdateRequest = {
  postActionMetricValue: string
  outcomeStatus: 'MEASURED_IMPROVEMENT' | 'NO_MATERIAL_CHANGE' | 'INCONCLUSIVE'
  outcomeNotes: string
}

export type Persona = 'EXECUTIVE' | 'PAYMENTS_PRODUCT' | 'RISK_COMPLIANCE' | 'TECH_OPS'
