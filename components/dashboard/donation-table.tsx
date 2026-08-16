'use client'

import { Trash2 } from 'lucide-react'

interface Donation {
  id: string
  donationId: string
  donorName: string
  itemName: string
  category: string
  quantity: number
  date: string
  status: 'Received' | 'Pending'
}

interface DonationTableProps {
  donations: Donation[]
  onDelete: (id: string) => void
}

export default function DonationTable({ donations, onDelete }: DonationTableProps) {
  const getStatusColor = (status: string) => {
    return status === 'Received' ? 'bg-green-100 text-green-800' : 'bg-amber-100 text-amber-800'
  }

  return (
    <div className="overflow-x-auto rounded-lg border border-border bg-card">
      <table className="w-full">
        <thead>
          <tr className="border-b border-border bg-secondary">
            <th className="px-6 py-3 text-left text-sm font-semibold text-foreground">Donation ID</th>
            <th className="px-6 py-3 text-left text-sm font-semibold text-foreground">Donor Name</th>
            <th className="px-6 py-3 text-left text-sm font-semibold text-foreground">Item Name</th>
            <th className="px-6 py-3 text-left text-sm font-semibold text-foreground">Category</th>
            <th className="px-6 py-3 text-left text-sm font-semibold text-foreground">Quantity</th>
            <th className="px-6 py-3 text-left text-sm font-semibold text-foreground">Date</th>
            <th className="px-6 py-3 text-left text-sm font-semibold text-foreground">Status</th>
            <th className="px-6 py-3 text-left text-sm font-semibold text-foreground">Action</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-border">
          {donations.length === 0 ? (
            <tr>
              <td colSpan={8} className="px-6 py-8 text-center text-muted-foreground">
                No donations found
              </td>
            </tr>
          ) : (
            donations.map((donation) => (
              <tr key={donation.id} className="hover:bg-secondary/50 transition-colors">
                <td className="px-6 py-4 text-sm font-medium text-foreground">{donation.donationId}</td>
                <td className="px-6 py-4 text-sm text-foreground">{donation.donorName}</td>
                <td className="px-6 py-4 text-sm text-foreground">{donation.itemName}</td>
                <td className="px-6 py-4 text-sm">
                  <span className="inline-block rounded-full bg-primary/10 px-3 py-1 text-primary text-xs font-medium">
                    {donation.category}
                  </span>
                </td>
                <td className="px-6 py-4 text-sm text-foreground">{donation.quantity}</td>
                <td className="px-6 py-4 text-sm text-foreground">{donation.date}</td>
                <td className="px-6 py-4 text-sm">
                  <span className={`inline-block rounded-full px-3 py-1 text-xs font-medium ${getStatusColor(donation.status)}`}>
                    {donation.status}
                  </span>
                </td>
                <td className="px-6 py-4 text-sm">
                  <button
                    onClick={() => onDelete(donation.id)}
                    className="text-destructive hover:text-red-700 transition-colors p-1 rounded hover:bg-red-50"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  )
}
