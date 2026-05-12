// ─── Enums ────────────────────────────────────────────────────────────────────

export type PaymentStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'REFUNDED'
export type PaymentMethod = 'CREDIT_CARD' | 'DEBIT_CARD' | 'BANK_TRANSFER' | 'DIGITAL_WALLET'

// ─── API Wrapper ──────────────────────────────────────────────────────────────

export interface ApiResponse<T> {
  success: boolean
  message?: string
  data: T
  errorCode?: string
  timestamp: string
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number       // current page (0-indexed)
  size: number
  first: boolean
  last: boolean
  empty: boolean
}

// ─── Auth ─────────────────────────────────────────────────────────────────────

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  username: string
  email: string
  roles: string[]
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
  firstName: string
  lastName: string
}

// ─── Payments ─────────────────────────────────────────────────────────────────

export interface PaymentRequest {
  amount: number
  currency: string
  paymentMethod: PaymentMethod
  payerId: string
  payeeId: string
  description?: string
  metadata?: Record<string, string>
}

export interface PaymentResponse {
  paymentId: string
  idempotencyKey: string
  amount: number
  currency: string
  paymentMethod: PaymentMethod
  status: PaymentStatus
  payerId: string
  payeeId: string
  description?: string
  metadata?: Record<string, string>
  failureReason?: string
  createdAt: string
  updatedAt: string
  cachedResponse: boolean
}

// ─── Transactions ─────────────────────────────────────────────────────────────

export interface TransactionDTO {
  transactionId: string
  paymentId: string
  userId: string
  amount: number
  currency: string
  paymentMethod: PaymentMethod
  status: PaymentStatus
  payerId: string
  payeeId: string
  description?: string
  failureReason?: string
  createdAt: string
  updatedAt: string
}
