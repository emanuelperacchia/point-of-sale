import type { DashboardAlert } from '../../services/dashboardApi';

interface AlertsBannerProps {
  alerts: DashboardAlert[];
}

const severityStyles: Record<string, string> = {
  HIGH: 'border-red-400 bg-red-50 text-red-800',
  MEDIUM: 'border-yellow-400 bg-yellow-50 text-yellow-800',
  LOW: 'border-blue-300 bg-blue-50 text-blue-800',
};

const typeIcons: Record<string, string> = {
  CRITICAL_STOCK: '⚠️',
  OVERDUE_RECEIVABLE: '💰',
  EXCESSIVE_WASTE: '♻️',
  PENDING_PAYROLL: '📋',
};

export default function AlertsBanner({ alerts }: AlertsBannerProps) {
  if (!alerts || alerts.length === 0) {
    return (
      <div className="rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700">
        ✅ No hay alertas activas
      </div>
    );
  }

  return (
    <div className="space-y-2">
      {alerts.map((alert, idx) => (
        <div
          key={`${alert.type}-${idx}`}
          className={`flex items-center gap-3 rounded-lg border-l-4 px-4 py-3 text-sm ${
            severityStyles[alert.severity] || severityStyles.LOW
          }`}
        >
          <span className="text-base">{typeIcons[alert.type] || '🔔'}</span>
          <span className="flex-1">{alert.message}</span>
          {alert.actionLink && (
            <a
              href={alert.actionLink}
              className="shrink-0 font-medium underline hover:no-underline"
            >
              Ver detalle
            </a>
          )}
          {alert.count && alert.count > 0 && (
            <span className="inline-flex h-5 min-w-[20px] items-center justify-center rounded-full bg-current px-1.5 text-xs font-bold text-white opacity-80">
              {alert.count}
            </span>
          )}
        </div>
      ))}
    </div>
  );
}
