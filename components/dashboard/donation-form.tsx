'use client'

import { useState } from 'react'
import { Button } from '@/components/ui/button'

interface DonationFormProps {
  onAddDonation: (donation: {
    donorName: string
    itemName: string
    category: 'Food' | 'Clothing' | 'Medicine' | 'Educational' | 'Household'
    quantity: number
    date: string
    notes?: string
  }) => void
}

export default function DonationForm({ onAddDonation }: DonationFormProps) {
  const [formData, setFormData] = useState({
    donorName: '',
    itemName: '',
    category: 'Food' as const,
    quantity: '',
    date: new Date().toISOString().split('T')[0],
    notes: '',
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (formData.donorName && formData.itemName && formData.quantity) {
      onAddDonation({
        donorName: formData.donorName,
        itemName: formData.itemName,
        category: formData.category,
        quantity: parseInt(formData.quantity),
        date: formData.date,
        notes: formData.notes,
      })
      setFormData({
        donorName: '',
        itemName: '',
        category: 'Food',
        quantity: '',
        date: new Date().toISOString().split('T')[0],
        notes: '',
      })
    }
  }

  const handleClear = () => {
    setFormData({
      donorName: '',
      itemName: '',
      category: 'Food',
      quantity: '',
      date: new Date().toISOString().split('T')[0],
      notes: '',
    })
  }

  return (
    <div className="rounded-lg border border-border bg-card p-6">
      <h3 className="text-lg font-bold text-foreground mb-6">Record New Donation</h3>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {/* Donation ID (auto-generated display) */}
          <div>
            <label className="block text-sm font-medium text-foreground mb-1">Donation ID</label>
            <input
              type="text"
              disabled
              value="Auto-generated"
              className="w-full rounded-lg border border-border bg-muted px-3 py-2 text-muted-foreground text-sm"
            />
          </div>

          {/* Donor Name */}
          <div>
            <label className="block text-sm font-medium text-foreground mb-1">Donor Name</label>
            <input
              type="text"
              required
              value={formData.donorName}
              onChange={(e) => setFormData({ ...formData, donorName: e.target.value })}
              placeholder="John Doe"
              className="w-full rounded-lg border border-border bg-card px-3 py-2 text-foreground placeholder-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          {/* Item Name */}
          <div>
            <label className="block text-sm font-medium text-foreground mb-1">Item Name</label>
            <input
              type="text"
              required
              value={formData.itemName}
              onChange={(e) => setFormData({ ...formData, itemName: e.target.value })}
              placeholder="Rice Bags"
              className="w-full rounded-lg border border-border bg-card px-3 py-2 text-foreground placeholder-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          {/* Category */}
          <div>
            <label className="block text-sm font-medium text-foreground mb-1">Category</label>
            <select
              value={formData.category}
              onChange={(e) => setFormData({ ...formData, category: e.target.value as any })}
              className="w-full rounded-lg border border-border bg-card px-3 py-2 text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="Food">Food</option>
              <option value="Clothing">Clothing</option>
              <option value="Medicine">Medicine</option>
              <option value="Educational">Educational</option>
              <option value="Household">Household</option>
            </select>
          </div>

          {/* Quantity */}
          <div>
            <label className="block text-sm font-medium text-foreground mb-1">Quantity</label>
            <input
              type="number"
              required
              min="1"
              value={formData.quantity}
              onChange={(e) => setFormData({ ...formData, quantity: e.target.value })}
              placeholder="50"
              className="w-full rounded-lg border border-border bg-card px-3 py-2 text-foreground placeholder-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          {/* Date */}
          <div>
            <label className="block text-sm font-medium text-foreground mb-1">Donation Date</label>
            <input
              type="date"
              value={formData.date}
              onChange={(e) => setFormData({ ...formData, date: e.target.value })}
              className="w-full rounded-lg border border-border bg-card px-3 py-2 text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
        </div>

        {/* Notes */}
        <div>
          <label className="block text-sm font-medium text-foreground mb-1">Notes</label>
          <textarea
            value={formData.notes}
            onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
            placeholder="Any additional notes about this donation..."
            rows={3}
            className="w-full rounded-lg border border-border bg-card px-3 py-2 text-foreground placeholder-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary resize-none"
          />
        </div>

        {/* Buttons */}
        <div className="flex gap-3 justify-end pt-4">
          <Button
            type="button"
            onClick={handleClear}
            className="px-4 py-2 rounded-lg border border-border bg-card text-foreground hover:bg-secondary"
          >
            Clear Form
          </Button>
          <Button
            type="submit"
            className="px-4 py-2 rounded-lg bg-primary text-primary-foreground hover:bg-accent"
          >
            Save Donation
          </Button>
        </div>
      </form>
    </div>
  )
}
