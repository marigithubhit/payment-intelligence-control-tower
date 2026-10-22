import type { ApiError } from '../types/api'

export const API_TARGET_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '')
// Vite proxies same-origin /api requests in development to avoid requiring backend CORS changes.
export const API_BASE_URL = import.meta.env.DEV ? '' : API_TARGET_URL

export class ApiClientError extends Error {
  status: number
  code?: string
  constructor(message: string, status: number, code?: string) { super(message); this.name = 'ApiClientError'; this.status = status; this.code = code }
}

export async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try { response = await fetch(`${API_BASE_URL}${path}`, init) }
  catch { throw new ApiClientError('Investigation service is currently unavailable. Check the backend connection and try again.', 0) }
  const text = await response.text()
  let body: unknown
  try { body = text ? JSON.parse(text) : null } catch { body = text }
  if (!response.ok) {
    const error = (body && typeof body === 'object' ? body : {}) as ApiError
    throw new ApiClientError(error.message || `Request failed with HTTP ${response.status}`, response.status, error.code)
  }
  return body as T
}
