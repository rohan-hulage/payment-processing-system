import { useQuery } from '@tanstack/react-query'
import { CreditCard, CheckCircle2, XCircle, Clock, TrendingUp } from 'lucide-react'
import { Link } from 'react-router-dom'
import { paymentService } from '../services/paymentService'
import { transactionService } from '../services/transactionService'
import { useAuthStore } from '../store/authStore'
import { StatusBadge } from '../components/ui/Badge'
import { Card, CardBody, CardHeader } from '../components/ui/Card'
import { PageSpinner } from '../components/ui/Spinner'
import { formatCurrency, formatDate } from '../lib/format'
import type { PaymentResponse } from '../types'

function StatCard({ label, value, icon: Icon, color }: {
  label: string; value: string | number; icon: typeof CreditCard; color: string
}) {
  return (
    <Card>
      <CardBody className="flex items-center gap-4">
        <div className={`flex items-center justify-center w-12 h-12 rounded-xl ${color}`}>
          <Icon className="h-6 w-6 text-white" />
        </div>
        <div>
          <p className="text-2xl font-bold text-gray-900">{value}</p>
          <p className="text-sm text-gray-500">{label}</p>
        </div>
      </CardBody>
    </Card>
  )
}

export function DashboardPage() {
  const user = useAuthStore((s) => s.user)

  const { data: payments, isLoading } = useQuery({
    queryKey: ['payments', 'dashboard'],
    queryFn: () => paymentService.list({ size: 5, sortDir: 'desc' }),
  })

  const { data: transactions } = useQuery({
    queryKey: ['transactions', 'dashboard'],
    queryFn: () => transactionService.list({ size: 5, sortDir: 'desc' }),
  })

  if (isLoading) return <PageSpinner />

  const all = payments?.content ?? []
  const completed = all.filter((p) => p.status === 'COMPLETED').length
  const failed    = all.filter((p) => p.status === 'FAILED').length
  const pending   = all.filter((p) => p.status === 'PENDING' || p.status === 'PROCESSING').length
  const totalAmt  = all.filter((p) => p.status === 'COMPLETED').reduce((s, p) => s + Number(p.amount), 0)

  return (
    <div className="p-8 space-y-8">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">
          Good {getGreeting()}, {user?.username} 👋
        </h1>
        <p className="text-gray-500 text-sm mt-1">Here's what's happening with your payments.</p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        <StatCard label="Total Payments"   value={payments?.totalElements ?? 0} icon={CreditCard}   color="bg-indigo-500" />
        <StatCard label="Completed"        value={completed}                    icon={CheckCircle2} color="bg-green-500" />
        <StatCard label="Failed"           value={failed}                       icon={XCircle}      color="bg-red-500" />
        <StatCard label="Pending"          value={pending}                      icon={Clock}        color="bg-yellow-500" />
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
        {/* Recent Payments */}
        <Card>
          <CardHeader className="flex items-center justify-between">
            <h2 className="font-semibold text-gray-900">Recent Payments</h2>
            <Link to="/payments" className="text-sm text-indigo-600 hover:underline">View all</Link>
          </CardHeader>
          <div className="divide-y divide-gray-50">
            {all.length === 0 ? (
              <CardBody><p className="text-sm text-gray-400 text-center py-4">No payments yet</p></CardBody>
            ) : (
              all.map((p) => <PaymentRow key={p.paymentId} payment={p} />)
            )}
          </div>
        </Card>

        {/* Recent Transactions */}
        <Card>
          <CardHeader className="flex items-center justify-between">
            <h2 className="font-semibold text-gray-900">Recent Transactions</h2>
            <Link to="/transactions" className="text-sm text-indigo-600 hover:underline">View all</Link>
          </CardHeader>
          <div className="divide-y divide-gray-50">
            {(transactions?.content ?? []).length === 0 ? (
              <CardBody><p className="text-sm text-gray-400 text-center py-4">No transactions yet</p></CardBody>
            ) : (
              transactions?.content.map((t) => (
                <div key={t.transactionId} className="px-6 py-3 flex items-center justify-between">
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-gray-900 truncate">{t.transactionId.slice(0, 8)}…</p>
                    <p className="text-xs text-gray-400">{formatDate(t.createdAt)}</p>
                  </div>
                  <div className="flex items-center gap-3 shrink-0">
                    <span className="text-sm font-semibold text-gray-900">
                      {formatCurrency(t.amount, t.currency)}
                    </span>
                    <StatusBadge status={t.status} />
                  </div>
                </div>
              ))
            )}
          </div>
        </Card>
      </div>

      {/* Volume card */}
      <Card>
        <CardBody className="flex items-center gap-4">
          <div className="flex items-center justify-center w-12 h-12 rounded-xl bg-indigo-500">
            <TrendingUp className="h-6 w-6 text-white" />
          </div>
          <div>
            <p className="text-sm text-gray-500">Total completed volume (last 5)</p>
            <p className="text-2xl font-bold text-gray-900">{formatCurrency(totalAmt, 'USD')}</p>
          </div>
        </CardBody>
      </Card>
    </div>
  )
}

function PaymentRow({ payment }: { payment: PaymentResponse }) {
  return (
    <Link to={`/payments/${payment.paymentId}`} className="block px-6 py-3 hover:bg-gray-50 transition-colors">
      <div className="flex items-center justify-between">
        <div className="min-w-0">
          <p className="text-sm font-medium text-gray-900 truncate">{payment.description || 'Payment'}</p>
          <p className="text-xs text-gray-400">{formatDate(payment.createdAt)}</p>
        </div>
        <div className="flex items-center gap-3 shrink-0">
          <span className="text-sm font-semibold text-gray-900">
            {formatCurrency(payment.amount, payment.currency)}
          </span>
          <StatusBadge status={payment.status} />
        </div>
      </div>
    </Link>
  )
}

function getGreeting() {
  const h = new Date().getHours()
  if (h < 12) return 'morning'
  if (h < 17) return 'afternoon'
  return 'evening'
}
