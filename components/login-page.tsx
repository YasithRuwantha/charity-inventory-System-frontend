'use client'

import { useState } from 'react'
import { Heart, Mail, Lock, CheckCircle2 } from 'lucide-react'
import { Button } from '@/components/ui/button'

interface LoginPageProps {
  onLogin: (role: 'admin' | 'staff' | 'volunteer') => void
}

const demoCredentials = [
  { role: 'Admin', email: 'admin@charity.org', password: 'password123', type: 'admin' as const },
  { role: 'Inventory Staff', email: 'staff@charity.org', password: 'password123', type: 'staff' as const },
  { role: 'Volunteer', email: 'volunteer@charity.org', password: 'password123', type: 'volunteer' as const },
]

export default function LoginPage({ onLogin }: LoginPageProps) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<{ email?: string; password?: string }>({})

  const validateForm = () => {
    const newErrors: { email?: string; password?: string } = {}

    if (!email) {
      newErrors.email = 'Email is required'
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      newErrors.email = 'Invalid email format'
    }

    if (!password) {
      newErrors.password = 'Password is required'
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault()
    if (validateForm()) {
      const foundUser = demoCredentials.find((cred) => cred.email === email && cred.password === password)
      if (foundUser) {
        onLogin(foundUser.type)
      } else {
        setErrors({ email: 'Invalid credentials' })
      }
    }
  }

  const fillCredentials = (cred: (typeof demoCredentials)[0]) => {
    setEmail(cred.email)
    setPassword(cred.password)
    setErrors({})
  }

  return (
    <div className="flex min-h-screen bg-background">
      {/* Left Panel - Illustration */}
      <div className="hidden flex-1 flex-col justify-between bg-gradient-to-br from-primary/10 to-accent/10 p-8 lg:flex">
        <div className="flex items-center gap-3">
          <div className="rounded-lg bg-primary p-2">
            <Heart className="h-6 w-6 text-primary-foreground" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-foreground">Charity Inventory</h1>
            <p className="text-sm text-muted-foreground">Management System</p>
          </div>
        </div>

        <div className="space-y-4">
          <h2 className="text-4xl font-bold text-foreground text-balance">
            Helping Communities Through Better Donation Management
          </h2>
          <p className="text-lg text-muted-foreground max-w-md">
            Track, manage, and distribute donations efficiently. Make a real difference in your community.
          </p>
        </div>

        <div className="space-y-6">
          <div className="space-y-3">
            <div className="flex items-start gap-3">
              <CheckCircle2 className="h-5 w-5 text-primary mt-0.5 flex-shrink-0" />
              <div>
                <p className="font-semibold text-foreground">Centralized Tracking</p>
                <p className="text-sm text-muted-foreground">Monitor all donations in one place</p>
              </div>
            </div>
            <div className="flex items-start gap-3">
              <CheckCircle2 className="h-5 w-5 text-primary mt-0.5 flex-shrink-0" />
              <div>
                <p className="font-semibold text-foreground">Real-time Updates</p>
                <p className="text-sm text-muted-foreground">Instant status and inventory changes</p>
              </div>
            </div>
            <div className="flex items-start gap-3">
              <CheckCircle2 className="h-5 w-5 text-primary mt-0.5 flex-shrink-0" />
              <div>
                <p className="font-semibold text-foreground">Secure & Reliable</p>
                <p className="text-sm text-muted-foreground">Your data is protected and backed up</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Right Panel - Login Form */}
      <div className="flex flex-1 flex-col justify-center px-4 py-8 sm:px-6 lg:px-8">
        <div className="mx-auto w-full max-w-md space-y-8">
          {/* Header */}
          <div className="flex lg:hidden items-center gap-3 mb-8">
            <div className="rounded-lg bg-primary p-2">
              <Heart className="h-6 w-6 text-primary-foreground" />
            </div>
            <div>
              <h1 className="text-xl font-bold text-foreground">Charity Inventory</h1>
              <p className="text-xs text-muted-foreground">Management System</p>
            </div>
          </div>

          {/* Login Card */}
          <div className="space-y-6">
            <div className="space-y-2">
              <h2 className="text-3xl font-bold text-foreground">Welcome Back</h2>
              <p className="text-muted-foreground">Sign in to your account to continue</p>
            </div>

            <form onSubmit={handleLogin} className="space-y-4">
              {/* Email Field */}
              <div className="space-y-2">
                <label htmlFor="email" className="block text-sm font-medium text-foreground">
                  Email Address
                </label>
                <div className="relative">
                  <Mail className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-muted-foreground" />
                  <input
                    id="email"
                    type="email"
                    value={email}
                    onChange={(e) => {
                      setEmail(e.target.value)
                      if (errors.email) setErrors({ ...errors, email: undefined })
                    }}
                    placeholder="you@example.com"
                    className={`w-full bg-card rounded-lg border-2 py-2 pl-10 pr-4 text-foreground placeholder-muted-foreground transition-colors focus:outline-none focus:ring-2 focus:ring-primary ${
                      errors.email ? 'border-destructive' : 'border-border'
                    }`}
                  />
                </div>
                {errors.email && <p className="text-sm text-destructive">{errors.email}</p>}
              </div>

              {/* Password Field */}
              <div className="space-y-2">
                <label htmlFor="password" className="block text-sm font-medium text-foreground">
                  Password
                </label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-muted-foreground" />
                  <input
                    id="password"
                    type="password"
                    value={password}
                    onChange={(e) => {
                      setPassword(e.target.value)
                      if (errors.password) setErrors({ ...errors, password: undefined })
                    }}
                    placeholder="••••••••"
                    className={`w-full bg-card rounded-lg border-2 py-2 pl-10 pr-4 text-foreground placeholder-muted-foreground transition-colors focus:outline-none focus:ring-2 focus:ring-primary ${
                      errors.password ? 'border-destructive' : 'border-border'
                    }`}
                  />
                </div>
                {errors.password && <p className="text-sm text-destructive">{errors.password}</p>}
              </div>

              {/* Remember Me & Forgot Password */}
              <div className="flex items-center justify-between">
                <label className="flex items-center gap-2 text-sm text-muted-foreground cursor-pointer">
                  <input type="checkbox" className="rounded" />
                  Remember me
                </label>
                <a href="#" className="text-sm text-primary hover:text-accent">
                  Forgot password?
                </a>
              </div>

              {/* Login Button */}
              <Button
                type="submit"
                className="w-full bg-primary text-primary-foreground hover:bg-accent py-2 rounded-lg font-semibold transition-colors"
              >
                Sign In
              </Button>
            </form>

            {/* Demo Credentials */}
            <div className="space-y-3 pt-4 border-t border-border">
              <p className="text-xs font-semibold text-muted-foreground uppercase">Demo Credentials</p>
              <div className="grid gap-2">
                {demoCredentials.map((cred) => (
                  <button
                    key={cred.email}
                    onClick={() => fillCredentials(cred)}
                    className="rounded-lg border border-border bg-card p-3 text-left transition-all hover:border-primary hover:bg-secondary"
                  >
                    <p className="font-medium text-foreground text-sm">{cred.role}</p>
                    <p className="text-xs text-muted-foreground">{cred.email}</p>
                    <p className="text-xs text-muted-foreground">Password: {cred.password}</p>
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
