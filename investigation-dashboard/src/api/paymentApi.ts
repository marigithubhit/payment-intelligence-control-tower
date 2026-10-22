import { apiRequest } from './client'
import type { PaymentIngestionResponse } from '../types/api'
export const ingestPaymentXml = (xml: string) => apiRequest<PaymentIngestionResponse>('/api/v1/payments/iso20022', { method: 'POST', headers: { 'Content-Type': 'application/xml', Accept: 'application/json' }, body: xml })
