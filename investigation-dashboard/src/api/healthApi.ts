import { apiRequest } from './client'
import type { Health } from '../types/api'
export const getHealth = () => apiRequest<Health>('/api/v1/health')
