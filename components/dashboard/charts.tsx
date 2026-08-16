'use client'

import { LineChart, Line, BarChart, Bar, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts'

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

interface ChartsProps {
  donations: Donation[]
}

const categoryColors: Record<string, string> = {
  Food: '#F97316',
  Clothing: '#3B82F6',
  Medicine: '#EF4444',
  Educational: '#A855F7',
  Household: '#22C55E',
}

export default function Charts({ donations }: ChartsProps) {
  // Donation trend data (last 7 days simulation)
  const trendData = [
    { day: 'Mon', donations: 45 },
    { day: 'Tue', donations: 52 },
    { day: 'Wed', donations: 48 },
    { day: 'Thu', donations: 61 },
    { day: 'Fri', donations: 55 },
    { day: 'Sat', donations: 67 },
    { day: 'Sun', donations: 72 },
  ]

  // Category distribution
  const categoryData = [
    { name: 'Food', value: donations.filter((d) => d.category === 'Food').reduce((sum, d) => sum + d.quantity, 0) },
    { name: 'Clothing', value: donations.filter((d) => d.category === 'Clothing').reduce((sum, d) => sum + d.quantity, 0) },
    { name: 'Medicine', value: donations.filter((d) => d.category === 'Medicine').reduce((sum, d) => sum + d.quantity, 0) },
    { name: 'Educational', value: donations.filter((d) => d.category === 'Educational').reduce((sum, d) => sum + d.quantity, 0) },
    { name: 'Household', value: donations.filter((d) => d.category === 'Household').reduce((sum, d) => sum + d.quantity, 0) },
  ].filter((item) => item.value > 0)

  // Status distribution
  const statusData = [
    { name: 'Received', value: donations.filter((d) => d.status === 'Received').length },
    { name: 'Pending', value: donations.filter((d) => d.status === 'Pending').length },
  ]

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      {/* Donation Trend Chart */}
      <div className="rounded-lg border border-border bg-card p-6">
        <h3 className="text-lg font-bold text-foreground mb-4">Donation Trend</h3>
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={trendData}>
            <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" />
            <XAxis dataKey="day" stroke="var(--color-muted-foreground)" />
            <YAxis stroke="var(--color-muted-foreground)" />
            <Tooltip
              contentStyle={{ backgroundColor: 'var(--color-card)', border: '1px solid var(--color-border)' }}
              labelStyle={{ color: 'var(--color-foreground)' }}
            />
            <Legend />
            <Line type="monotone" dataKey="donations" stroke="var(--color-primary)" strokeWidth={2} dot={{ fill: 'var(--color-primary)' }} />
          </LineChart>
        </ResponsiveContainer>
      </div>

      {/* Category Distribution Pie Chart */}
      <div className="rounded-lg border border-border bg-card p-6">
        <h3 className="text-lg font-bold text-foreground mb-4">Category Distribution</h3>
        <ResponsiveContainer width="100%" height={300}>
          <PieChart>
            <Pie
              data={categoryData}
              cx="50%"
              cy="50%"
              labelLine={false}
              label={({ name, value }) => `${name}: ${value}`}
              outerRadius={100}
              fill="var(--color-primary)"
              dataKey="value"
            >
              {categoryData.map((entry) => (
                <Cell key={`cell-${entry.name}`} fill={categoryColors[entry.name] || 'var(--color-primary)'} />
              ))}
            </Pie>
            <Tooltip
              contentStyle={{ backgroundColor: 'var(--color-card)', border: '1px solid var(--color-border)' }}
              labelStyle={{ color: 'var(--color-foreground)' }}
            />
          </PieChart>
        </ResponsiveContainer>
      </div>

      {/* Status Distribution Bar Chart */}
      <div className="rounded-lg border border-border bg-card p-6">
        <h3 className="text-lg font-bold text-foreground mb-4">Status Distribution</h3>
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={statusData}>
            <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" />
            <XAxis dataKey="name" stroke="var(--color-muted-foreground)" />
            <YAxis stroke="var(--color-muted-foreground)" />
            <Tooltip
              contentStyle={{ backgroundColor: 'var(--color-card)', border: '1px solid var(--color-border)' }}
              labelStyle={{ color: 'var(--color-foreground)' }}
            />
            <Legend />
            <Bar dataKey="value" fill="var(--color-primary)" name="Count" />
          </BarChart>
        </ResponsiveContainer>
      </div>

      {/* Monthly Summary */}
      <div className="rounded-lg border border-border bg-card p-6">
        <h3 className="text-lg font-bold text-foreground mb-4">Summary Stats</h3>
        <div className="space-y-4">
          <div className="flex justify-between items-center pb-3 border-b border-border">
            <span className="text-muted-foreground">Total Items Donated</span>
            <span className="text-2xl font-bold text-primary">{donations.reduce((sum, d) => sum + d.quantity, 0)}</span>
          </div>
          <div className="flex justify-between items-center pb-3 border-b border-border">
            <span className="text-muted-foreground">Unique Donors</span>
            <span className="text-2xl font-bold text-primary">{new Set(donations.map((d) => d.donorName)).size}</span>
          </div>
          <div className="flex justify-between items-center pb-3 border-b border-border">
            <span className="text-muted-foreground">Total Donations</span>
            <span className="text-2xl font-bold text-primary">{donations.length}</span>
          </div>
          <div className="flex justify-between items-center">
            <span className="text-muted-foreground">Completion Rate</span>
            <span className="text-2xl font-bold text-primary">
              {((donations.filter((d) => d.status === 'Received').length / donations.length) * 100).toFixed(0)}%
            </span>
          </div>
        </div>
      </div>
    </div>
  )
}
