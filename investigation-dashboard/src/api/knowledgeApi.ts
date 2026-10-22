import { apiRequest } from './client'
import type { KnowledgeSearchResponse } from '../types/api'
export const searchKnowledge = (query: string) => apiRequest<KnowledgeSearchResponse>(`/api/v1/knowledge/search?q=${encodeURIComponent(query)}`)
