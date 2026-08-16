'use client'

import { useState } from 'react'
import { LogOut, Menu, X, Plus, Trash2, Search } from 'lucide-react'
import { Button } from '@/components/ui/button'
import SummaryCards from './dashboard/summary-cards'
import DonationForm from './dashboard/donation-form'
import DonationTable from './dashboard/donation-table'
import CategoryGrid from './dashboard/category-grid'
import ActivityPanel from './dashboard/activity-panel'
import Charts from './dashboard/charts'

interface DashboardProps {
  onLogout: () => void
  userRole: 'admin' | 'staff' | 'volunteer'
}

interface Donation {
  id: string
  donationId: string
  donorName: string
  itemName: string
  category: 'Food' | 'Clothing' | 'Medicine' | 'Educational' | 'Household'
  quantity: number
  date: string
  status: 'Received' | 'Pending'
}

const initialDonations: Donation[] = [
  {
    id: '1',
    donationId: 'DON-001',
    donorName: 'John Perera',
    itemName: 'Rice Bags',
    category: 'Food',
    quantity: 50,
    date: '2026-06-10',
    status: 'Received',
  },
  {
    id: '2',
    donationId: 'DON-002',
    donorName: 'Mary Silva',
    itemName: 'School Books',
    category: 'Educational',
    quantity: 120,
    date: '2026-06-11',
    status: 'Received',
  },
  {
    id: '3',
    donationId: 'DON-003',
    donorName: 'Red Cross',
    itemName: 'First Aid Kits',
    category: 'Medicine',
    quantity: 30,
    date: '2026-06-12',
    status: 'Pending',
  },
  {
    id: '4',
    donationId: 'DON-004',
    donorName: 'ABC Supermarket',
    itemName: 'Canned Food',
    category: 'Food',
    quantity: 75,
    date: '2026-06-13',
    status: 'Received',
  },
]

export default function Dashboard({ onLogout, userRole }: DashboardProps) {
  const [donations, setDonations] = useState<Donation[]>(initialDonations)
  const [sidebarOpen, setSidebarOpen] = useState(true)
  const [activeTab, setActiveTab] = useState<'overview' | 'donations' | 'inventory' | 'distribution' | 'reports'>(
    'overview'
  )
  const [searchQuery, setSearchQuery] = useState('')
  const [filterCategory, setFilterCategory] = useState<string>('all')
  const [filterStatus, setFilterStatus] = useState<string>('all')

  const addDonation = (donation: Omit<Donation, 'id' | 'donationId' | 'status'>) => {
    const newId = (Math.max(...donations.map((d) => parseInt(d.id)), 0) + 1).toString()
    const newDonationId = `DON-${String(donations.length + 1).padStart(3, '0')}`
    setDonations([...donations, { ...donation, id: newId, donationId: newDonationId, status: 'Received' }])
  }

  const deleteDonation = (id: string) => {
    setDonations(donations.filter((d) => d.id !== id))
  }

  const filteredDonations = donations.filter((donation) => {
    const matchesSearch =
      donation.donorName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      donation.itemName.toLowerCase().includes(searchQuery.toLowerCase())
    const matchesCategory = filterCategory === 'all' || donation.category === filterCategory
    const matchesStatus = filterStatus === 'all' || donation.status === filterStatus
    return matchesSearch && matchesCategory && matchesStatus
  })

  const getCategoryCount = (category: string) => {
    return donations.filter((d) => d.category === category).reduce((sum, d) => sum + d.quantity, 0)
  }

  const stats = {
    totalDonations: donations.reduce((sum, d) => sum + d.quantity, 0),
    categories: new Set(donations.map((d) => d.category)).size,
    thisMonth: donations.filter((d) => d.date.includes('2026-06')).reduce((sum, d) => sum + d.quantity, 0),
    pending: donations.filter((d) => d.status === 'Pending').length,
  }

  const navItems = [
    { id: 'overview', label: 'Dashboard', icon: '📊' },
    { id: 'donations', label: 'Donations', icon: '🎁' },
    { id: 'inventory', label: 'Inventory', icon: '📦' },
    { id: 'distribution', label: 'Distribution', icon: '🚚' },
    { id: 'reports', label: 'Reports', icon: '📈' },
  ]

  return (
    <div className="flex h-screen bg-background">
      {/* Sidebar */}
      <div
        className={`fixed inset-y-0 left-0 z-50 w-64 bg-card border-r border-border transition-transform lg:relative lg:translate-x-0 ${
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="flex h-full flex-col">
          {/* Logo */}
          <div className="flex items-center gap-3 border-b border-border p-6">
            <div className="rounded-lg bg-primary p-2">
              <span className="text-xl text-primary-foreground">❤️</span>
            </div>
            <div>
              <h1 className="font-bold text-foreground">Charity Mgmt</h1>
              <p className="text-xs text-muted-foreground">System</p>
            </div>
          </div>

          {/* Navigation */}
          <nav className="flex-1 space-y-2 p-4">
            {navItems.map((item) => (
              <button
                key={item.id}
                onClick={() => {
                  setActiveTab(item.id as any)
                  setSidebarOpen(false)
                }}
                className={`w-full flex items-center gap-3 rounded-lg px-4 py-3 text-left transition-colors ${
                  activeTab === item.id
                    ? 'bg-primary text-primary-foreground'
                    : 'text-foreground hover:bg-secondary'
                }`}
              >
                <span className="text-xl">{item.icon}</span>
                <span className="font-medium">{item.label}</span>
              </button>
            ))}
          </nav>

          {/* Profile & Logout */}
          <div className="border-t border-border p-4 space-y-3">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-full bg-primary flex items-center justify-center text-primary-foreground font-bold">
                V
              </div>
              <div className="flex-1">
                <p className="font-medium text-foreground text-sm">Volunteer User</p>
                <p className="text-xs text-muted-foreground capitalize">{userRole}</p>
              </div>
            </div>
            <Button
              onClick={onLogout}
              className="w-full bg-destructive text-primary-foreground hover:bg-red-700 flex items-center justify-center gap-2 py-2 rounded-lg"
            >
              <LogOut className="h-4 w-4" />
              Logout
            </Button>
          </div>
        </div>
      </div>

      {/* Mobile Sidebar Overlay */}
      {sidebarOpen && (
        <div className="fixed inset-0 z-40 bg-black/50 lg:hidden" onClick={() => setSidebarOpen(false)} />
      )}

      {/* Main Content */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Top Navigation */}
        <div className="border-b border-border bg-card px-4 py-4 sm:px-6 flex items-center justify-between">
          <button
            onClick={() => setSidebarOpen(!sidebarOpen)}
            className="rounded-lg p-2 text-muted-foreground hover:bg-secondary lg:hidden"
          >
            {sidebarOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
          </button>
          <h2 className="text-xl font-bold text-foreground capitalize flex-1 ml-4 lg:ml-0">
            {navItems.find((item) => item.id === activeTab)?.label || 'Dashboard'}
          </h2>
          <div className="flex items-center gap-2">
            <span className="text-sm text-muted-foreground">Volunteer User</span>
            <div className="h-8 w-8 rounded-full bg-primary flex items-center justify-center text-primary-foreground text-sm font-bold">
              V
            </div>
          </div>
        </div>

        {/* Content Area */}
        <div className="flex-1 overflow-auto">
          {activeTab === 'overview' && (
            <div className="space-y-6 p-4 sm:p-6">
              {/* Summary Cards */}
              <SummaryCards stats={stats} />

              {/* Charts Section */}
              <Charts donations={donations} />

              {/* Category Grid */}
              <div>
                <h3 className="text-lg font-bold text-foreground mb-4">Donation Categories</h3>
                <CategoryGrid categories={['Food', 'Clothing', 'Medicine', 'Educational', 'Household']} getCategoryCount={getCategoryCount} />
              </div>

              {/* Activity Panel */}
              <ActivityPanel donations={donations} />
            </div>
          )}

          {activeTab === 'donations' && (
            <div className="space-y-6 p-4 sm:p-6">
              {/* Add Donation Form */}
              <DonationForm onAddDonation={addDonation} />

              {/* Donation Table */}
              <div>
                <div className="mb-4 space-y-3">
                  <h3 className="text-lg font-bold text-foreground">Donation History</h3>
                  <div className="flex flex-col sm:flex-row gap-2">
                    <div className="flex-1 relative">
                      <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                      <input
                        type="text"
                        placeholder="Search donor name or item..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        className="w-full bg-card rounded-lg border border-border py-2 pl-10 pr-4 text-foreground placeholder-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                      />
                    </div>
                    <select
                      value={filterCategory}
                      onChange={(e) => setFilterCategory(e.target.value)}
                      className="bg-card rounded-lg border border-border px-3 py-2 text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                    >
                      <option value="all">All Categories</option>
                      <option value="Food">Food</option>
                      <option value="Clothing">Clothing</option>
                      <option value="Medicine">Medicine</option>
                      <option value="Educational">Educational</option>
                      <option value="Household">Household</option>
                    </select>
                    <select
                      value={filterStatus}
                      onChange={(e) => setFilterStatus(e.target.value)}
                      className="bg-card rounded-lg border border-border px-3 py-2 text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                    >
                      <option value="all">All Status</option>
                      <option value="Received">Received</option>
                      <option value="Pending">Pending</option>
                    </select>
                  </div>
                </div>
                <DonationTable donations={filteredDonations} onDelete={deleteDonation} />
              </div>
            </div>
          )}

          {activeTab === 'inventory' && (
            <div className="p-4 sm:p-6">
              <h3 className="text-lg font-bold text-foreground mb-4">Inventory Management</h3>
              <p className="text-muted-foreground">Inventory management features coming soon...</p>
            </div>
          )}

          {activeTab === 'distribution' && (
            <div className="p-4 sm:p-6">
              <h3 className="text-lg font-bold text-foreground mb-4">Distribution</h3>
              <p className="text-muted-foreground">Distribution tracking features coming soon...</p>
            </div>
          )}

          {activeTab === 'reports' && (
            <div className="p-4 sm:p-6">
              <h3 className="text-lg font-bold text-foreground mb-4">Reports</h3>
              <p className="text-muted-foreground">Advanced reporting features coming soon...</p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
