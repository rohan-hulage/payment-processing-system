import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Plus, Filter } from 'lucide-react'
import { paymentService } from '../services/paymentService'
import { StatusBadge } from '../components/ui/Badge'
import { Button } from '../components/ui/Button'
import { Card, CardBody } from '../components/ui/Card'
import { Select } from '../components/ui/Select'
import { PageSpinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { NewPaymentModal } from '../components/payments/NewPaymentModal'
import { formatCurrency, formatDate, formatPaymentMethod } from '../lib/format'
import type { PaymentStatus } from '../types'

const STATUS_OPTIONS = [
  { value: '',           label: 'All statuses' },
  { value: 'PENDING',    label: 'Pending' },
  { value: 'PROCESSING', label: 'Processing' },
  { value: 'COMPLETED',  label: 'Completed' },
  { value: 'FAILED',     label: 'Failed' },
  { value: 'REFUNDED',   label: 'Refunded' },
]

export function PaymentsPage() {
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState<PaymentStatus | ''>('')
  const [showModal, setShowModal] = useState(false)
  const queryClient = useQueryClient()

  const { data, isLoading, error } = useQuery({
    queryKey: ['payments', page, status],
    queryFn: () => paymentService.list({ page, size: 10, status: status || undefined, sortDir: 'desc' }),
  })

  const cancelMutation = useMutation({
    mutationFn: paymentService.cancel,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['payments'] }),
  })

  if (isLoading) return <PageSpinner />

  return (
    <div className="p-8 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Payments</h1>
          <p className="text-sm text-gray-500 mt-1">{data?.totalElements ?? 0} total payments</p>
        </div>
        <Button onClick={() => setShowModal(true)}>
          <Plus className="h-4 w-4" />
          New Payment
        </Button>
      </div>

      {/* Filters */}
      <Card>
        <CardBody className="flex items-center gap-4">
          <Filter className="h-4 w-4 text-gray-400 shrink-0" />
          <Select
            options={STATUS_OPTIONS}
            value={status}
            onChange={(e) => { setStatus(e.target.value as PaymentStatus | ''); setPage(0) }}
            className="w-48"
          />
        </CardBody>
      </Card>

      {error && <Alert variant="error">Failed to load payments. Please try again.</Alert>}

      {/* Table */}
      <Card>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-100 bg-gray-50">
                <th className="text-left px-6 py-3 font-medium text-gray-500">Payment ID</th>
                <th className="text-left px-6 py-3 font-medium text-gray-500">Description</th>
                <th className="text-left px-6 py-3 font-medium text-gray-500">Method</th>
                <th className="text-right px-6 py-3 font-medium text-gray-500">Amount</th>
                <th className="text-left px-6 py-3 font-medium text-gray-500">Status</th>
                <th className="text-left px-6 py-3 font-medium text-gray-500">Date</th>
                <th className="px-6 py-3" />
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {data?.content.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center text-gray-400">
                    No payments found
                  </td>
                </tr>
              ) : (
                data?.content.map((p) => (
                  <tr key={p.paymentId} className="hover:bg-gray-50 transition-colors">
                    <td className="px-6 py-4 font-mono text-xs text-gray-500">
                      <Link to={`/payments/${p.paymentId}`} className="text-indigo-600 hover:underline">
                        {p.paymentId.slice(0, 8)}…
                      </Link>
                    </td>
                    <td className="px-6 py-4 text-gray-900 max-w-[180px] truncate">
                      {p.description || '—'}
                    </td>
                    <td className="px-6 py-4 text-gray-600">{formatPaymentMethod(p.paymentMethod)}</td>
                    <td className="px-6 py-4 text-right font-semibold text-gray-900">
                      {formatCurrency(p.amount, p.currency)}
                    </td>
                    <td className="px-6 py-4"><StatusBadge status={p.status} /></td>
                    <td className="px-6 py-4 text-gray-500 whitespace-nowrap">{formatDate(p.createdAt)}</td>
                    <td className="px-6 py-4">
                      {p.status === 'PENDING' && (
                        <Button
                          variant="danger"
                          size="sm"
                          loading={cancelMutation.isPending && cancelMutation.variables === p.paymentId}
                          onClick={() => cancelMutation.mutate(p.paymentId)}
                        >
                          Cancel
                        </Button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {data && data.totalPages > 1 && (
          <div className="px-6 py-4 border-t border-gray-100 flex items-center justify-between">
            <p className="text-sm text-gray-500">
              Page {data.number + 1} of {data.totalPages}
            </p>
            <div className="flex gap-2">
              <Button variant="secondary" size="sm" disabled={data.first} onClick={() => setPage(p => p - 1)}>
                Previous
              </Button>
              <Button variant="secondary" size="sm" disabled={data.last} onClick={() => setPage(p => p + 1)}>
                Next
              </Button>
            </div>
          </div>
        )}
      </Card>

      {showModal && (
        <NewPaymentModal
          onClose={() => setShowModal(false)}
          onSuccess={() => {
            setShowModal(false)
            queryClient.invalidateQueries({ queryKey: ['payments'] })
          }}
        />
      )}
    </div>
  )
}
