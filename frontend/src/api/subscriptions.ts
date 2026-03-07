import { apiFetch } from './client'
import type { SubscriptionResponse, SubscriptionRequest, SubscriptionUpdateRequest } from './types'

const BASE = '/api/v1/subscriptions'

export const getSubscriptions = () =>
  apiFetch<SubscriptionResponse[]>(BASE)

export const getSubscription = (id: number) =>
  apiFetch<SubscriptionResponse>(`${BASE}/${id}`)

export const createSubscription = (data: SubscriptionRequest) =>
  apiFetch<SubscriptionResponse>(BASE, { method: 'POST', body: JSON.stringify(data) })

export const updateSubscription = (id: number, data: SubscriptionUpdateRequest) =>
  apiFetch<SubscriptionResponse>(`${BASE}/${id}`, { method: 'PUT', body: JSON.stringify(data) })

export const updateSubscriptionStatus = (id: number, status: 'ACTIVE' | 'PAUSED') =>
  apiFetch<SubscriptionResponse>(`${BASE}/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  })

export const deleteSubscription = (id: number) =>
  apiFetch<void>(`${BASE}/${id}`, { method: 'DELETE' })
