import type { SellerRankingItem } from '../../services/dashboardApi';

interface TopSellersListProps {
  items: SellerRankingItem[];
}

export default function TopSellersList({ items }: TopSellersListProps) {
  if (!items || items.length === 0) {
    return (
      <div className="rounded-lg bg-surface p-4 ">
        <h3 className="mb-3 text-sm font-semibold uppercase tracking-wider text-ink-faint">
          Top Vendedores
        </h3>
        <p className="text-sm text-ink-faint">Sin datos en el período</p>
      </div>
    );
  }

  const maxAmount = Math.max(...items.map((s) => s.totalAmount), 1);

  return (
    <div className="rounded-lg bg-surface p-4 ">
      <h3 className="mb-3 text-sm font-semibold uppercase tracking-wider text-ink-faint">
        Top Vendedores
      </h3>
      <div className="space-y-3">
        {items.map((item, idx) => (
          <div key={item.employeeId}>
            <div className="flex items-center justify-between text-sm">
              <div className="flex items-center gap-2 min-w-0">
                <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-gray-200 text-xs font-bold text-ink-soft">
                  {idx + 1}
                </span>
                <span className="truncate font-medium text-ink">{item.employeeName}</span>
              </div>
              <div className="flex items-center gap-3 shrink-0">
                <span className="text-xs text-ink-faint">{item.transactionCount} ventas</span>
                <span className="font-semibold text-ink">
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
            <div className="mt-1 h-1.5 w-full rounded-full bg-surface-raised/30">
              <div
                className="h-1.5 rounded-full bg-green-500 transition-all"
                style={{ width: `${(item.totalAmount / maxAmount) * 100}%` }}
              />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}


