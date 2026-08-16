'use client'

interface Donation {
  id: string
  donationId: string
  donorName: string
  itemName: string
  quantity: number
  date: string
}

interface ActivityPanelProps {
  donations: Donation[]
}

const timeOfDay = (date: string) => {
  const hours = Math.floor(Math.random() * 24)
  const minutes = Math.floor(Math.random() * 60)
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`
}

export default function ActivityPanel({ donations }: ActivityPanelProps) {
  const recentActivities = donations.slice(-4).map((d) => ({
    ...d,
    time: timeOfDay(d.date),
  }))

  return (
    <div className="rounded-lg border border-border bg-card p-6">
      <h3 className="text-lg font-bold text-foreground mb-6">Recent Activity</h3>

      <div className="space-y-4">
        {recentActivities.map((activity, index) => (
          <div key={activity.id} className="flex gap-4">
            {/* Timeline dot */}
            <div className="relative flex flex-col items-center">
              <div className="h-3 w-3 rounded-full bg-primary mt-1.5" />
              {index < recentActivities.length - 1 && (
                <div className="h-12 w-0.5 bg-border my-2" />
              )}
            </div>

            {/* Activity content */}
            <div className="flex-1 pb-4">
              <p className="text-xs font-semibold text-muted-foreground uppercase">{activity.time}</p>
              <p className="text-foreground font-medium mt-1">
                {activity.donorName} donated {activity.quantity} {activity.itemName}
              </p>
              <p className="text-sm text-muted-foreground mt-0.5">{activity.date}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
