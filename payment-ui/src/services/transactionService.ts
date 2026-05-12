import api from '../lib/axios'
import type { ApiResponse, Page, PaymentStatus, TransactionDTO } from '../types'

export interface ListTransactionsParams {
  status?: PaymentStatus
  page?: number
  size?: number
  sortBy?: string
  sortDir?: 'asc' | 'desc'
}

export const transactionService = {
  getById: async (transactionId: string): Promise<TransactionDTO> => {
    const res = await api.get<ApiResponse<TransactionDTO>>(`/transactions/${transactionId}`)
    return res.data.data
  },

  list: async (params: ListTransactionsParams = {}): Promise<Page<TransactionDTO>> => {
    const res = await api.get<ApiResponse<Page<TransactionDTO>>>('/transactions', { params })
    return res.data.data
  },

  listByPayment: async (paymentId: string): Promise<TransactionDTO[]> => {
    const res = await api.get<ApiResponse<TransactionDTO[]>>(`/transactions/payment/${paymentId}`)
    return res.data.data
  },
}
