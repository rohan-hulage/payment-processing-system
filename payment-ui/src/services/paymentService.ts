import { v4 as uuidv4 } from 'uuid'
import api from '../lib/axios'
import type { ApiResponse, Page, PaymentRequest, PaymentResponse, PaymentStatus } from '../types'

export interface ListPaymentsParams {
  status?: PaymentStatus
  page?: number
  size?: number
  sortBy?: string
  sortDir?: 'asc' | 'desc'
}

export const paymentService = {
  initiate: async (data: PaymentRequest): Promise<PaymentResponse> => {
    const idempotencyKey = uuidv4()
    const res = await api.post<ApiResponse<PaymentResponse>>('/payments', data, {
      headers: { 'X-Idempotency-Key': idempotencyKey },
    })
    return res.data.data
  },

  getById: async (paymentId: string): Promise<PaymentResponse> => {
    const res = await api.get<ApiResponse<PaymentResponse>>(`/payments/${paymentId}`)
    return res.data.data
  },

  list: async (params: ListPaymentsParams = {}): Promise<Page<PaymentResponse>> => {
    const res = await api.get<ApiResponse<Page<PaymentResponse>>>('/payments', { params })
    return res.data.data
  },

  cancel: async (paymentId: string): Promise<PaymentResponse> => {
    const res = await api.delete<ApiResponse<PaymentResponse>>(`/payments/${paymentId}`)
    return res.data.data
  },
}
