'use client'

import { TrendingUp, Package, AlertCircle, Layers } from 'lucide-react'

interface SummaryCardsProps {
  stats: {
    totalDonations: number
    categories: number
    thisMonth: number
    pending: number
  }
}

export default function SummaryCards({ stats }: SummaryCardsProps) {
  const cards = [
    {
      title: 'Total Donations',
      value: stats.totalDonations.toLocaleString(),
      icon: Package,
      color: 'bg-primary/10 text-primary',
    },
    {
      title: 'Donation Categories',
      value: stats.categories,
      icon: Layers,
      color: 'bg-accent/10 text-accent',
    },
    {
      title: 'Items This Month',
      value: stats.thisMonth.toLocaleString(),
      icon: TrendingUp,
      color: 'bg-primary/5 text-primary',
    },
    {
      title: 'Pending Donations',
      value: stats.pending,
      icon: AlertCircle,
      color: 'bg-amber-100 text-amber-600',
    },
  ]

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {cards.map((card, index) => {
        const Icon = card.icon
        return (
          <div key={index} className="rounded-lg border border-border bg-card p-6 space-y-4 hover:shadow-md transition-shadow">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-medium text-muted-foreground">{card.title}</h3>
              <div className={`${card.color} rounded-lg p-2`}>
                <Icon className="h-5 w-5" />
              </div>
            </div>
            <p className="text-3xl font-bold text-foreground">{card.value}</p>
          </div>
        )
      })}
    </div>
  )
}
