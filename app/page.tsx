'use client'

import { useState } from 'react'
import LoginPage from '@/components/login-page'
import Dashboard from '@/components/dashboard'

export default function Page() {
  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const [userRole, setUserRole] = useState<'admin' | 'staff' | 'volunteer'>('volunteer')

  const handleLogin = (role: 'admin' | 'staff' | 'volunteer') => {
    setUserRole(role)
    setIsLoggedIn(true)
  }

  if (!isLoggedIn) {
    return <LoginPage onLogin={handleLogin} />
  }

  return <Dashboard onLogout={() => setIsLoggedIn(false)} userRole={userRole} />
}
