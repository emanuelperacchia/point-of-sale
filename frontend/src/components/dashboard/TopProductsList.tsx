import type { ProductRankingItem } from '../../services/dashboardApi';

interface TopProductsListProps {
  items: ProductRankingItem[];
}

export default function TopProductsList({ items }: TopProductsListProps) {
  if (!items || items.length === 0) {
    return (
      <div className="rounded-lg bg-white p-4 shadow-sm">
        <h3 className="mb-3 text-sm font-semibold uppercase tracking-wider text-gray-500">
          Top Productos
        </h3>
        <p className="text-sm text-gray-400">Sin datos en el período</p>
      </div>
    );
  }

  const maxAmount = Math.max(...items.map((p) => p.totalAmount), 1);

  return (
    <div className="rounded-lg bg-white p-4 shadow-sm">
      <h3 className="mb-3 text-sm font-semibold uppercase tracking-wider text-gray-500">
        Top Productos
      </h3>
      <div className="space-y-3">
        {items.map((item, idx) => (
          <div key={item.productId}>
            <div className="flex items-center justify-between text-sm">
              <div className="flex items-center gap-2 min-w-0">
                <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-gray-200 text-xs font-bold text-gray-600">
                  {idx + 1}
                </span>
                <span className="truncate font-medium text-gray-800">{item.productName}</span>
                <span className="shrink-0 text-xs text-gray-400">{item.productSku}</span>
              </div>
              <div className="flex items-center gap-3 shrink-0">
                <span className="font-semibold text-gray-900">
                  ${item.totalAmount.toLocaleString('es-AR')}
                </span>
                {item.variation !== 0 && (
                  <span
                    className={`text-xs font-medium ${
                      item.variation >= 0 ? 'text-green-600' : 'text-red-500'
                    }`}
                  >
                    {item.variation >= 0 ? '+' : ''}
                    {item.variation.toFixed(1)}%
                  </span>
                )}
              </div>
            </div>
            {/* Barra visual */}
            <div className="mt-1 h-1.5 w-full rounded-full bg-gray-100">
              <div
                className="h-1.5 rounded-full bg-blue-500 transition-all"
                style={{ width: `${(item.totalAmount / maxAmount) * 100}%` }}
              />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
