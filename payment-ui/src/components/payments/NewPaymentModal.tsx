import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation } from '@tanstack/react-query'
import { X } from 'lucide-react'
import { paymentService } from '../../services/paymentService'
import { useAuthStore } from '../../store/authStore'
import { Input } from '../ui/Input'
import { Select } from '../ui/Select'
import { Button } from '../ui/Button'
import { Alert } from '../ui/Alert'
import type { AxiosError } from 'axios'
import type { ApiResponse, PaymentMethod } from '../../types'

const CURRENCIES = ['USD', 'EUR', 'GBP', 'JPY', 'CAD', 'AUD', 'CHF', 'CNY', 'INR', 'SGD']
const METHODS: { value: PaymentMethod; label: string }[] = [
  { value: 'CREDIT_CARD',    label: 'Credit Card' },
  { value: 'DEBIT_CARD',     label: 'Debit Card' },
  { value: 'BANK_TRANSFER',  label: 'Bank Transfer' },
  { value: 'DIGITAL_WALLET', label: 'Digital Wallet' },
]

const schema = z.object({
  amount:        z.string().min(1, 'Amount is required'),
  currency:      z.string().min(3).max(3),
  paymentMethod: z.enum(['CREDIT_CARD', 'DEBIT_CARD', 'BANK_TRANSFER', 'DIGITAL_WALLET']),
  payerId:       z.string().min(1, 'Payer ID is required').max(64),
  payeeId:       z.string().min(1, 'Payee ID is required').max(64),
  description:   z.string().max(255).optional(),
})
type FormData = z.infer<typeof schema>

interface Props {
  onClose: () => void
  onSuccess: () => void
}

export function NewPaymentModal({ onClose, onSuccess }: Props) {
  const user = useAuthStore((s) => s.user)

  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      currency: 'USD',
      paymentMethod: 'CREDIT_CARD',
      payerId: user?.username ?? '',
    },
  })

  const mutation = useMutation({
    mutationFn: paymentService.initiate,
    onSuccess,
  })

  const onSubmit = (data: FormData) => {
    const amountNum = parseFloat(data.amount)
    if (isNaN(amountNum) || amountNum < 0.01 || amountNum > 999999.99) return
    mutation.mutate({ ...data, amount: amountNum })
  }

  const errorMsg = mutation.error
    ? ((mutation.error as AxiosError<ApiResponse<null>>).response?.data?.message ?? 'Payment failed.')
    : null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
          <h2 className="text-lg font-semibold text-gray-900">New Payment</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 transition-colors">
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Body */}
        <form onSubmit={handleSubmit(onSubmit)} className="px-6 py-5 space-y-4" noValidate>
          {errorMsg && <Alert variant="error">{errorMsg}</Alert>}

          {mutation.isSuccess && (
            <Alert variant="success">
              Payment initiated successfully!{' '}
              {mutation.data?.cachedResponse && '(Duplicate — cached response returned)'}
            </Alert>
          )}

          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Amount"
              type="number"
              step="0.01"
              placeholder="99.99"
              error={errors.amount?.message}
              {...register('amount')}
            />
            <Select
              label="Currency"
              options={CURRENCIES.map((c) => ({ value: c, label: c }))}
              error={errors.currency?.message}
              {...register('currency')}
            />
          </div>

          <Select
            label="Payment Method"
            options={METHODS}
            error={errors.paymentMethod?.message}
            {...register('paymentMethod')}
          />

          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Payer ID"
              placeholder="user-123"
              error={errors.payerId?.message}
              {...register('payerId')}
            />
            <Input
              label="Payee ID"
              placeholder="merchant-456"
              error={errors.payeeId?.message}
              {...register('payeeId')}
            />
          </div>

          <Input
            label="Description (optional)"
            placeholder="Order #789"
            error={errors.description?.message}
            {...register('description')}
          />

          <div className="flex gap-3 pt-2">
            <Button type="button" variant="secondary" className="flex-1" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" loading={mutation.isPending} className="flex-1">
              Initiate Payment
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
