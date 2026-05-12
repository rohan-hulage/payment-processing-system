import { Loader2 } from 'lucide-react'

export function Spinner({ className = '' }: { className?: string }) {
  return <Loader2 className={`animate-spin text-indigo-600 ${className}`} />
}

export function PageSpinner() {
  return (
    <div className="flex items-center justify-center min-h-[300px]">
      <Spinner className="h-8 w-8" />
    </div>
  )
}
