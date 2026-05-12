import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { Wallet } from 'lucide-react'
import { authService } from '../services/authService'
import { useAuthStore } from '../store/authStore'
import { Input } from '../components/ui/Input'
import { Button } from '../components/ui/Button'
import { Alert } from '../components/ui/Alert'
import type { AxiosError } from 'axios'
import type { ApiResponse } from '../types'

const schema = z.object({
  firstName: z.string().min(1, 'First name is required').max(64),
  lastName:  z.string().min(1, 'Last name is required').max(64),
  username:  z.string().min(3, 'At least 3 characters').max(64)
    .regex(/^[a-zA-Z0-9_.-]+$/, 'Letters, digits, underscores, dots, hyphens only'),
  email:    z.string().email('Invalid email address'),
  password: z.string()
    .min(8, 'At least 8 characters')
    .regex(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])/, 'Must include uppercase, lowercase, digit, and special character (@$!%*?&)'),
})
type FormData = z.infer<typeof schema>

export function RegisterPage() {
  const navigate = useNavigate()
  const setAuth = useAuthStore((s) => s.setAuth)

  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  })

  const mutation = useMutation({
    mutationFn: authService.register,
    onSuccess: (data) => {
      setAuth(data)
      navigate('/dashboard', { replace: true })
    },
  })

  const onSubmit = (data: FormData) => mutation.mutate(data)

  const errorMsg = mutation.error
    ? ((mutation.error as AxiosError<ApiResponse<null>>).response?.data?.message ?? 'Registration failed.')
    : null

  return (
    <div className="min-h-screen bg-gradient-to-br from-indigo-50 to-blue-100 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="flex flex-col items-center mb-8">
          <div className="flex items-center justify-center w-14 h-14 rounded-2xl bg-indigo-600 mb-4 shadow-lg">
            <Wallet className="h-7 w-7 text-white" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900">Create account</h1>
          <p className="text-gray-500 text-sm mt-1">Join PayFlow today</p>
        </div>

        <div className="bg-white rounded-2xl shadow-xl p-8">
          {errorMsg && (
            <div className="mb-5">
              <Alert variant="error">{errorMsg}</Alert>
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
            <div className="grid grid-cols-2 gap-4">
              <Input label="First name" placeholder="John" error={errors.firstName?.message} {...register('firstName')} />
              <Input label="Last name"  placeholder="Doe"  error={errors.lastName?.message}  {...register('lastName')} />
            </div>
            <Input
              label="Username"
              placeholder="john_doe"
              autoComplete="username"
              error={errors.username?.message}
              {...register('username')}
            />
            <Input
              label="Email"
              type="email"
              placeholder="john@example.com"
              autoComplete="email"
              error={errors.email?.message}
              {...register('email')}
            />
            <Input
              label="Password"
              type="password"
              placeholder="••••••••"
              autoComplete="new-password"
              hint="Min 8 chars with uppercase, lowercase, digit & special char"
              error={errors.password?.message}
              {...register('password')}
            />
            <Button type="submit" loading={mutation.isPending} className="w-full" size="lg">
              Create account
            </Button>
          </form>

          <p className="text-center text-sm text-gray-500 mt-6">
            Already have an account?{' '}
            <Link to="/login" className="text-indigo-600 font-medium hover:underline">
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
