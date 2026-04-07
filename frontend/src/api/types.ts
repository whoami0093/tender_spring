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

// ── Tenders ──────────────────────────────────────────────────────────────────

export type TenderStatus = 'SENT' | 'WON' | 'LOST' | 'IN_PROGRESS'

export interface TenderResponse {
  id: number
  purchaseNumber: string
  title: string
  region: string | null
  customer: string | null
  customerInn: string | null
  amount: number | null
  currency: string
  status: TenderStatus
  deadline: string | null
  publishedAt: string | null
  eisUrl: string | null
  takenInWork: boolean
  createdAt: string
  updatedAt: string
}

export interface TenderPageResponse {
  content: TenderResponse[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

export interface TenderFilter {
  region?: string
  customer?: string
  status?: TenderStatus[]
  takenInWork?: boolean
  deadlineFrom?: string
  deadlineTo?: string
  amountFrom?: number
  amountTo?: number
  numberSearch?: string
  page?: number
  size?: number
  sort?: string
}
