import type { PaymentStatus } from '../../types'

const statusConfig: Record<PaymentStatus, { label: string; className: string }> = {
  PENDING:    { label: 'Pending',    className: 'bg-yellow-100 text-yellow-800 border-yellow-200' },
  PROCESSING: { label: 'Processing', className: 'bg-blue-100 text-blue-800 border-blue-200' },
  COMPLETED:  { label: 'Completed',  className: 'bg-green-100 text-green-800 border-green-200' },
  FAILED:     { label: 'Failed',     className: 'bg-red-100 text-red-800 border-red-200' },
  REFUNDED:   { label: 'Refunded',   className: 'bg-purple-100 text-purple-800 border-purple-200' },
}

export function StatusBadge({ status }: { status: PaymentStatus }) {
  const { label, className } = statusConfig[status] ?? { label: status, className: 'bg-gray-100 text-gray-800' }
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border ${className}`}>
      {label}
    </span>
  )
}
