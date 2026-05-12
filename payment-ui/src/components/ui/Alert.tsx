import { AlertCircle, CheckCircle2, Info, XCircle } from 'lucide-react'
import type { ReactNode } from 'react'

type AlertVariant = 'success' | 'error' | 'info' | 'warning'

const config: Record<AlertVariant, { icon: typeof Info; className: string }> = {
  success: { icon: CheckCircle2, className: 'bg-green-50 border-green-200 text-green-800' },
  error:   { icon: XCircle,      className: 'bg-red-50 border-red-200 text-red-800' },
  info:    { icon: Info,         className: 'bg-blue-50 border-blue-200 text-blue-800' },
  warning: { icon: AlertCircle,  className: 'bg-yellow-50 border-yellow-200 text-yellow-800' },
}

export function Alert({ variant = 'info', children }: { variant?: AlertVariant; children: ReactNode }) {
  const { icon: Icon, className } = config[variant]
  return (
    <div className={`flex items-start gap-3 rounded-lg border p-4 text-sm ${className}`} role="alert">
      <Icon className="h-5 w-5 shrink-0 mt-0.5" />
      <div>{children}</div>
    </div>
  )
}
