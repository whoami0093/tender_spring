export interface SubscriptionFilters {
  regions: number[]
  keywords: string[]
  localKeywords: string[]
  customerInn?: string
  maxPriceFrom?: number
  maxPriceTo?: number
}

export interface SubscriptionResponse {
  id: number
  source: string
  label?: string
  emails: string[]
  status: 'ACTIVE' | 'PAUSED'
  filters: SubscriptionFilters
  lastCheckedAt?: string
  createdAt: string
}

export interface SubscriptionRequest {
  source: string
  label?: string
  emails: string[]
  filters: SubscriptionFilters
}

export interface SubscriptionUpdateRequest {
  label?: string
  emails: string[]
  filters: SubscriptionFilters
}
