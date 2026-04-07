import { apiFetch } from './client'
import type { TenderFilter, TenderPageResponse, TenderResponse } from './types'

function buildQuery(filter: TenderFilter): string {
  const params = new URLSearchParams()
  if (filter.region) params.set('region', filter.region)
  if (filter.customer) params.set('customer', filter.customer)
  if (filter.status?.length) params.set('status', filter.status.join(','))
  if (filter.takenInWork !== undefined) params.set('takenInWork', String(filter.takenInWork))
  if (filter.deadlineFrom) params.set('deadlineFrom', filter.deadlineFrom)
  if (filter.deadlineTo) params.set('deadlineTo', filter.deadlineTo)
  if (filter.amountFrom !== undefined) params.set('amountFrom', String(filter.amountFrom))
  if (filter.amountTo !== undefined) params.set('amountTo', String(filter.amountTo))
  if (filter.numberSearch) params.set('numberSearch', filter.numberSearch)
  params.set('page', String(filter.page ?? 0))
  params.set('size', String(filter.size ?? 20))
  params.set('sort', filter.sort ?? 'createdAt,desc')
  return params.toString()
}

export async function getTenders(filter: TenderFilter): Promise<TenderPageResponse> {
  return apiFetch<TenderPageResponse>(`/api/v1/tenders?${buildQuery(filter)}`)
}

export async function getTender(id: number): Promise<TenderResponse> {
  return apiFetch<TenderResponse>(`/api/v1/tenders/${id}`)
}

export async function patchTender(id: number, takenInWork: boolean): Promise<TenderResponse> {
  return apiFetch<TenderResponse>(`/api/v1/tenders/${id}`, {
    method: 'PATCH',
    body: JSON.stringify({ takenInWork }),
  })
}
