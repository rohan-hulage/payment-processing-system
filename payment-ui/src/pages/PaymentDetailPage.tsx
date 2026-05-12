import { useParams, Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Copy, Check } from 'lucide-react'
import { useState } from 'react'
import { paymentService } from '../services/paymentService'
import { transactionService } from '../services/transactionService'
import { StatusBadge } from '../components/ui/Badge'
import { Button } from '../components/ui/Button'
import { Card, CardHeader, CardBody } from '../components/ui/Card'
import { PageSpinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { formatCurrency, formatDate, formatPaymentMethod } from '../lib/format'
import type { ReactNode } from 'react'

function CopyButton({ text }: { text: string }) {
  const [copied, setCopied] = useState(false)
  const copy = () => {
    navigator.clipboard.writeText(text)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }
  return (
    <button onClick={copy} className="text-gray-400 hover:text-gray-600 transition-colors ml-1">
      {copied ? <Check className="h-3.5 w-3.5 text-green-500" /> : <Copy className="h-3.5 w-3.5" />}
    </button>
  )
}

function DetailRow({ label, value, mono = false }: { label: string; value: ReactNode; mono?: boolean }) {
  return (
    <div className="flex items-start justify-between py-3 border-b border-gray-50 last:border-0">
      <span className="text-sm text-gray-500 shrink-0 w-40">{label}</span>
      <span className={`text-sm text-gray-900 text-right ${mono ? 'font-mono text-xs' : 'font-medium'}`}>
        {value}
      </span>
    </div>
  )
}

export function PaymentDetailPage() {
  const { paymentId } = useParams<{ paymentId: string }>()
  const queryClient = useQueryClient()

  const { data: payment, isLoading, error } = useQuery({
    queryKey: ['payment', paymentId],
    queryFn: () => paymentService.getById(paymentId!),
    enabled: !!paymentId,
  })

  const { data: transactions } = useQuery({
    queryKey: ['transactions', 'payment', paymentId],
    queryFn: () => transactionService.listByPayment(paymentId!),
    enabled: !!paymentId,
  })

  const cancelMutation = useMutation({
    mutationFn: () => paymentService.cancel(paymentId!),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['payment', paymentId] }),
  })

  if (isLoading) return <PageSpinner />
  if (error || !payment) return (
    <div className="p-8">
      <Alert variant="error">Payment not found or you don't have access.</Alert>
    </div>
  )

  return (
    <div className="p-8 space-y-6 max-w-3xl">
      {/* Back */}
      <Link to="/payments" className="inline-flex items-center gap-2 text-sm text-gray-500 hover:text-gray-700">
        <ArrowLeft className="h-4 w-4" />
        Back to Payments
      </Link>

      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Payment Details</h1>
          <div className="flex items-center gap-1 mt-1">
            <span className="font-mono text-xs text-gray-400">{payment.paymentId}</span>
            <CopyButton text={payment.paymentId} />
          </div>
        </div>
        <div className="flex items-center gap-3">
          <StatusBadge status={payment.status} />
          {payment.status === 'PENDING' && (
            <Button
              variant="danger"
              size="sm"
              loading={cancelMutation.isPending}
              onClick={() => cancelMutation.mutate()}
            >
              Cancel Payment
            </Button>
          )}
        </div>
      </div>

      {cancelMutation.isSuccess && <Alert variant="success">Payment cancelled successfully.</Alert>}
      {cancelMutation.isError && <Alert variant="error">Failed to cancel payment.</Alert>}

      {/* Amount hero */}
      <Card>
        <CardBody className="text-center py-8">
          <p className="text-4xl font-bold text-gray-900">
            {formatCurrency(payment.amount, payment.currency)}
          </p>
          <p className="text-gray-500 mt-1">{payment.description || 'No description'}</p>
        </CardBody>
      </Card>

      {/* Details */}
      <Card>
        <CardHeader><h2 className="font-semibold text-gray-900">Payment Information</h2></CardHeader>
        <CardBody>
          <DetailRow label="Payment ID"      value={<span className="flex items-center">{payment.paymentId.slice(0,8)}… <CopyButton text={payment.paymentId} /></span>} />
          <DetailRow label="Idempotency Key" value={<span className="flex items-center">{payment.idempotencyKey.slice(0,8)}… <CopyButton text={payment.idempotencyKey} /></span>} />
          <DetailRow label="Status"          value={<StatusBadge status={payment.status} />} />
          <DetailRow label="Amount"          value={formatCurrency(payment.amount, payment.currency)} />
          <DetailRow label="Currency"        value={payment.currency} />
          <DetailRow label="Method"          value={formatPaymentMethod(payment.paymentMethod)} />
          <DetailRow label="Payer ID"        value={payment.payerId} mono />
          <DetailRow label="Payee ID"        value={payment.payeeId} mono />
          <DetailRow label="Created"         value={formatDate(payment.createdAt)} />
          <DetailRow label="Updated"         value={formatDate(payment.updatedAt)} />
          {payment.failureReason && (
            <DetailRow label="Failure Reason" value={<span className="text-red-600">{payment.failureReason}</span>} />
          )}
          {payment.cachedResponse && (
            <DetailRow label="Note" value={<span className="text-yellow-600">Cached response (duplicate request)</span>} />
          )}
        </CardBody>
      </Card>

      {/* Metadata */}
      {payment.metadata && Object.keys(payment.metadata).length > 0 && (
        <Card>
          <CardHeader><h2 className="font-semibold text-gray-900">Metadata</h2></CardHeader>
          <CardBody>
            {Object.entries(payment.metadata).map(([k, v]) => (
              <DetailRow key={k} label={k} value={v} mono />
            ))}
          </CardBody>
        </Card>
      )}

      {/* Transactions */}
      <Card>
        <CardHeader><h2 className="font-semibold text-gray-900">Transactions</h2></CardHeader>
        {!transactions || transactions.length === 0 ? (
          <CardBody><p className="text-sm text-gray-400 text-center py-4">No transactions recorded yet</p></CardBody>
        ) : (
          <div className="divide-y divide-gray-50">
            {transactions.map((t) => (
              <div key={t.transactionId} className="px-6 py-3 flex items-center justify-between">
                <div>
                  <p className="text-sm font-mono text-gray-500">{t.transactionId.slice(0, 8)}…</p>
                  <p className="text-xs text-gray-400">{formatDate(t.createdAt)}</p>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-sm font-semibold">{formatCurrency(t.amount, t.currency)}</span>
                  <StatusBadge status={t.status} />
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  )
}
