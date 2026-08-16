'use client'

interface CategoryGridProps {
  categories: string[]
  getCategoryCount: (category: string) => number
}

const categoryIcons: Record<string, string> = {
  Food: '🍽️',
  Clothing: '👕',
  Medicine: '💊',
  Educational: '📚',
  Household: '🏠',
}

const categoryColors: Record<string, string> = {
  Food: 'bg-orange-50 border-orange-200',
  Clothing: 'bg-blue-50 border-blue-200',
  Medicine: 'bg-red-50 border-red-200',
  Educational: 'bg-purple-50 border-purple-200',
  Household: 'bg-green-50 border-green-200',
}

const textColors: Record<string, string> = {
  Food: 'text-orange-700',
  Clothing: 'text-blue-700',
  Medicine: 'text-red-700',
  Educational: 'text-purple-700',
  Household: 'text-green-700',
}

export default function CategoryGrid({ categories, getCategoryCount }: CategoryGridProps) {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
      {categories.map((category) => (
        <div
          key={category}
          className={`rounded-lg border-2 p-6 text-center hover:shadow-md transition-shadow ${categoryColors[category] || 'bg-gray-50'}`}
        >
          <div className="text-4xl mb-3">{categoryIcons[category] || '📦'}</div>
          <h4 className={`font-semibold mb-2 ${textColors[category] || 'text-gray-700'}`}>{category}</h4>
          <p className={`text-2xl font-bold ${textColors[category] || 'text-gray-700'}`}>
            {getCategoryCount(category)} Items
          </p>
        </div>
      ))}
    </div>
  )
}
