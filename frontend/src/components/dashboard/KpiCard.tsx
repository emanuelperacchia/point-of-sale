interface KpiCardProps {
  title: string;
  value: string;
  variation?: number;
  subtitle?: string;
  status?: string;
  color?: 'blue' | 'green' | 'red' | 'yellow' | 'purple';
}

const colorMap = {
  blue: 'border-blue-500 bg-blue-50',
  green: 'border-green-500 bg-green-50',
  red: 'border-red-500 bg-red-50',
  yellow: 'border-yellow-500 bg-yellow-50',
  purple: 'border-purple-500 bg-purple-50',
};

const formatVariation = (v: number | undefined): string => {
  if (v === undefined || v === null) return '';
  const signo = v >= 0 ? '+' : '';
  return `${signo}${v.toFixed(1)}%`;
};

export default function KpiCard({ title, value, variation, subtitle, status, color = 'blue' }: KpiCardProps) {
  const isError = status === 'ERROR';
  const isPositive = variation !== undefined && variation >= 0;

  return (
    <div className={`rounded-lg border-l-4 p-4 shadow-sm ${colorMap[color]} ${isError ? 'opacity-60' : ''}`}>
      <p className="text-xs font-medium uppercase tracking-wider text-gray-500">{title}</p>
      <p className="mt-1 text-2xl font-bold text-gray-900">
        {isError ? '—' : value}
      </p>
      <div className="mt-2 flex items-center gap-2 text-xs">
        {variation !== undefined && !isError && (
          <span
            className={`inline-flex items-center rounded-full px-2 py-0.5 font-medium ${
              isPositive ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
            }`}
          >
            {isPositive ? '↑' : '↓'} {formatVariation(variation)}
          </span>
        )}
        {subtitle && <span className="text-gray-400">{subtitle}</span>}
        {isError && <span className="text-red-500 font-medium">Sin datos</span>}
      </div>
    </div>
  );
}
