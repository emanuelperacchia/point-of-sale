import { useEffect, useState } from 'react';
import Layout from '../components/common/Layout';
import KpiCard from '../components/dashboard/KpiCard';
import TopProductsList from '../components/dashboard/TopProductsList';
import TopSellersList from '../components/dashboard/TopSellersList';
import AlertsBanner from '../components/dashboard/AlertsBanner';
import { dashboardApi, type ExecutiveDashboardResponse } from '../services/dashboardApi';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';

const periodos = [
  { value: 'TODAY', label: 'Hoy' },
  { value: 'YESTERDAY', label: 'Ayer' },
  { value: 'WEEK', label: '7 días' },
  { value: 'MONTH', label: 'Mes' },
  { value: 'YTD', label: 'Año' },
];

const formatCurrency = (n: number) =>
  '$' + (n ?? 0).toLocaleString('es-AR', { minimumFractionDigits: 0, maximumFractionDigits: 0 });

const formatPercent = (n: number) => (n ?? 0).toFixed(1) + '%';

export default function DashboardPage() {
  const [data, setData] = useState<ExecutiveDashboardResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [periodo, setPeriodo] = useState('MONTH');
  const [error, setError] = useState<string | null>(null);

  const load = async (p: string) => {
    setLoading(true);
    setError(null);
    try {
      const res = await dashboardApi.getExecutive(p);
      setData(res.data);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error al cargar dashboard';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load(periodo);
  }, [periodo]);

  if (loading) {
    return (
      <Layout title="Dashboard Ejecutivo">
        <div className="flex items-center justify-center py-20">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent" />
        </div>
      </Layout>
    );
  }

  if (error) {
    return (
      <Layout title="Dashboard Ejecutivo">
        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      </Layout>
    );
  }

  return (
    <Layout title="Dashboard Ejecutivo">
      {/* Selector de período */}
      <div className="mb-6 flex items-center gap-2">
        <span className="text-sm font-medium text-gray-500">Período:</span>
        <div className="flex gap-1">
          {periodos.map((p) => (
            <button
              key={p.value}
              onClick={() => setPeriodo(p.value)}
              className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                periodo === p.value
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'bg-white text-gray-600 hover:bg-gray-100'
              }`}
            >
              {p.label}
            </button>
          ))}
        </div>
        <button
          onClick={() => load(periodo)}
          className="ml-2 rounded-md bg-gray-100 px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-200"
        >
          ↻
        </button>
      </div>

      {data && (
        <div className="space-y-6">
          {/* Alertas */}
          <AlertsBanner alerts={data.alerts} />

          {/* KPIs — 1ra fila: Ventas */}
          <div>
            <h3 className="mb-3 text-sm font-semibold uppercase tracking-wider text-gray-500">Ventas</h3>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <KpiCard
                title="Ventas Totales"
                value={formatCurrency(data.sales?.totalSales)}
                variation={data.sales?.salesVariation}
                status={data.sales?.status}
                color="blue"
              />
              <KpiCard
                title="Transacciones"
                value={String(data.sales?.transactionCount ?? '—')}
                variation={data.sales?.transactionVariation}
                status={data.sales?.status}
                color="green"
              />
              <KpiCard
                title="Ticket Promedio"
                value={formatCurrency(data.sales?.averageTicket)}
                status={data.sales?.status}
                color="purple"
              />
              <KpiCard
                title="Órdenes Producidas"
                value={String(data.production?.completedOrders ?? '—')}
                subtitle={`Merma: ${formatPercent(data.production?.averageWaste ?? 0)}`}
                status={data.production?.status}
                color="yellow"
              />
            </div>
          </div>

          {/* KPIs — 2da fila: Finanzas + Inventario + RRHH */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <KpiCard
              title="Ingresos"
              value={formatCurrency(data.financial?.income)}
              status={data.financial?.status}
              color="green"
            />
            <KpiCard
              title="Gastos"
              value={formatCurrency(data.financial?.expenses)}
              status={data.financial?.status}
              color="red"
            />
            <KpiCard
              title="Valor Inventario"
              value={formatCurrency(data.inventory?.totalStockValue)}
              subtitle={`${data.inventory?.criticalStockCount ?? 0} críticos`}
              status={data.inventory?.status}
              color="purple"
            />
            <KpiCard
              title="RRHH"
              value={`${data.hr?.activeEmployees ?? 0} activos`}
              subtitle={`Costo: ${formatCurrency(data.hr?.laborCost)}`}
              status={data.hr?.status}
              color="blue"
            />
          </div>

          {/* Gráfico de ventas diarias */}
          <div className="rounded-lg bg-white p-4 shadow-sm">
            <h3 className="mb-4 text-sm font-semibold uppercase tracking-wider text-gray-500">
              Ventas Diarias
            </h3>
            {data.dailySales && data.dailySales.length > 0 ? (
              <ResponsiveContainer width="100%" height={250}>
                <BarChart data={data.dailySales}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                  <XAxis
                    dataKey="date"
                    tick={{ fontSize: 11 }}
                    tickFormatter={(val: string) => {
                      const d = new Date(val + 'T00:00:00');
                      return d.toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit' });
                    }}
                  />
                  <YAxis tick={{ fontSize: 11 }} tickFormatter={(v: number) => '$' + (v / 1000).toFixed(0) + 'k'} />
                  <Tooltip
                    formatter={(value: number) => [formatCurrency(value), 'Ventas']}
                    labelFormatter={(label: string) => {
                      const d = new Date(label + 'T00:00:00');
                      return d.toLocaleDateString('es-AR', { day: '2-digit', month: 'long', year: 'numeric' });
                    }}
                  />
                  <Bar dataKey="amount" fill="#3b82f6" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <p className="text-sm text-gray-400">Sin datos de ventas en el período</p>
            )}
          </div>

          {/* Rankings */}
          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            <TopProductsList items={data.topProducts} />
            <TopSellersList items={data.topSellers} />
          </div>
        </div>
      )}
    </Layout>
  );
}
