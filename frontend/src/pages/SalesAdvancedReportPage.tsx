import { useEffect, useState } from 'react';
import Layout from '../components/common/Layout';
import api from '../services/api';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  LineChart, Line, Legend, PieChart, Pie, Cell,
} from 'recharts';

interface SalesReportResponse {
  resumen: {
    totalVentas: number;
    cantidadTransacciones: number;
    ticketPromedio: number;
    totalDevoluciones: number;
    descuentosAplicados: number;
    impuestosCobrados: number;
    variacionVsPeriodoAnterior: number;
    status: string;
  } | null;
  porMetodoPago: Array<{ metodo: string; monto: number; cantidad: number; porcentaje: number }>;
  ventasPorHora: Array<{ hora: number; monto: number; cantidad: number }>;
  ventasPorDiaSemana: Array<{ dia: string; diaNumero: number; monto: number; cantidad: number }>;
  comparativa: Array<{ fecha: string; montoActual: number; montoAnterior: number }>;
  periodo: string;
  periodoAnterior: string;
}

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'];

function formatCurrency(n: number) {
  return '$' + (n ?? 0).toLocaleString('es-AR', { minimumFractionDigits: 2 });
}

function formatPct(n: number) {
  return (n ?? 0).toFixed(1) + '%';
}

export default function SalesAdvancedReportPage() {
  const [data, setData] = useState<SalesReportResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [desde, setDesde] = useState(() => {
    const d = new Date(); d.setDate(d.getDate() - 30);
    return d.toISOString().split('T')[0];
  });
  const [hasta, setHasta] = useState(() => new Date().toISOString().split('T')[0]);

  const load = async (d: string, h: string) => {
    setLoading(true);
    try {
      const res = await api.get<SalesReportResponse>('/reports/sales-advanced', {
        params: { desde: d, hasta: h },
      });
      setData(res.data);
    } catch {
      // handled by interceptor
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load(desde, hasta);
  }, []);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    load(desde, hasta);
  };

  return (
    <Layout title="Reporte Avanzado de Ventas">
      {/* Filtros */}
      <form onSubmit={handleSearch} className="mb-6 flex flex-wrap items-end gap-3">
        <div>
          <label className="block text-xs font-medium text-gray-500">Desde</label>
          <input
            type="date"
            value={desde}
            onChange={(e) => setDesde(e.target.value)}
            className="mt-1 rounded-md border px-3 py-1.5 text-sm"
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-gray-500">Hasta</label>
          <input
            type="date"
            value={hasta}
            onChange={(e) => setHasta(e.target.value)}
            className="mt-1 rounded-md border px-3 py-1.5 text-sm"
          />
        </div>
        <button
          type="submit"
          className="rounded-md bg-blue-600 px-4 py-1.5 text-sm font-medium text-white hover:bg-blue-700"
        >
          Generar
        </button>
      </form>

      {loading && (
        <div className="flex justify-center py-12">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent" />
        </div>
      )}

      {data && (
        <div className="space-y-6">
          {/* Resumen */}
          {data.resumen && data.resumen.status === 'OK' && (
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-7">
              <MetricBox label="Total Ventas" value={formatCurrency(data.resumen.totalVentas)} />
              <MetricBox label="Transacciones" value={String(data.resumen.cantidadTransacciones)} />
              <MetricBox label="Ticket Prom." value={formatCurrency(data.resumen.ticketPromedio)} />
              <MetricBox label="Devoluciones" value={formatCurrency(data.resumen.totalDevoluciones)} />
              <MetricBox label="Descuentos" value={formatCurrency(data.resumen.descuentosAplicados)} />
              <MetricBox label="Impuestos" value={formatCurrency(data.resumen.impuestosCobrados)} />
              <MetricBox
                label="Vs. Período Ant."
                value={formatPct(data.resumen.variacionVsPeriodoAnterior)}
                color={data.resumen.variacionVsPeriodoAnterior >= 0 ? 'text-green-600' : 'text-red-500'}
              />
            </div>
          )}

          {/* Comparativa período anterior */}
          {data.comparativa && data.comparativa.length > 0 && (
            <div className="rounded-lg bg-white p-4 shadow-sm">
              <h3 className="mb-1 text-sm font-semibold uppercase tracking-wider text-gray-500">
                Comparativa vs Período Anterior
              </h3>
              <p className="mb-3 text-xs text-gray-400">
                Actual: {data.periodo} | Anterior: {data.periodoAnterior}
              </p>
              <ResponsiveContainer width="100%" height={250}>
                <LineChart data={data.comparativa}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                  <XAxis dataKey="fecha" tick={{ fontSize: 10 }} />
                  <YAxis tick={{ fontSize: 11 }} tickFormatter={(v: number) => '$' + (v / 1000).toFixed(0) + 'k'} />
                  <Tooltip formatter={(value: number) => formatCurrency(value)} />
                  <Legend />
                  <Line type="monotone" dataKey="montoActual" stroke="#3b82f6" name="Actual" strokeWidth={2} dot={false} />
                  <Line type="monotone" dataKey="montoAnterior" stroke="#9ca3af" name="Anterior" strokeWidth={2} dot={false} strokeDasharray="4 4" />
                </LineChart>
              </ResponsiveContainer>
            </div>
          )}

          {/* Ventas por hora */}
          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            <div className="rounded-lg bg-white p-4 shadow-sm">
              <h3 className="mb-3 text-sm font-semibold uppercase tracking-wider text-gray-500">
                Ventas por Hora
              </h3>
              {data.ventasPorHora.length > 0 ? (
                <ResponsiveContainer width="100%" height={220}>
                  <BarChart data={data.ventasPorHora}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                    <XAxis dataKey="hora" tick={{ fontSize: 11 }} />
                    <YAxis tick={{ fontSize: 11 }} />
                    <Tooltip formatter={(value: number) => formatCurrency(value)} />
                    <Bar dataKey="monto" fill="#3b82f6" radius={[3, 3, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <p className="text-sm text-gray-400">Sin datos</p>
              )}
            </div>

            {/* Ventas por día de semana */}
            <div className="rounded-lg bg-white p-4 shadow-sm">
              <h3 className="mb-3 text-sm font-semibold uppercase tracking-wider text-gray-500">
                Ventas por Día de Semana
              </h3>
              {data.ventasPorDiaSemana.length > 0 ? (
                <ResponsiveContainer width="100%" height={220}>
                  <BarChart data={data.ventasPorDiaSemana}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                    <XAxis dataKey="dia" tick={{ fontSize: 11 }} />
                    <YAxis tick={{ fontSize: 11 }} />
                    <Tooltip formatter={(value: number) => formatCurrency(value)} />
                    <Bar dataKey="monto" fill="#10b981" radius={[3, 3, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <p className="text-sm text-gray-400">Sin datos</p>
              )}
            </div>
          </div>

          {/* Método de pago */}
          {data.porMetodoPago.length > 0 && (
            <div className="rounded-lg bg-white p-4 shadow-sm">
              <h3 className="mb-3 text-sm font-semibold uppercase tracking-wider text-gray-500">
                Ventas por Método de Pago
              </h3>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <ResponsiveContainer width="100%" height={220}>
                  <PieChart>
                    <Pie
                      data={data.porMetodoPago}
                      dataKey="monto"
                      nameKey="metodo"
                      cx="50%"
                      cy="50%"
                      outerRadius={80}
                      label={({ metodo, porcentaje }: { metodo: string; porcentaje: number }) =>
                        `${metodo} ${porcentaje.toFixed(1)}%`
                      }
                    >
                      {data.porMetodoPago.map((_, idx) => (
                        <Cell key={idx} fill={COLORS[idx % COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip formatter={(value: number) => formatCurrency(value)} />
                  </PieChart>
                </ResponsiveContainer>
                <div className="space-y-2">
                  {data.porMetodoPago.map((pm, idx) => (
                    <div key={pm.metodo} className="flex items-center justify-between text-sm">
                      <div className="flex items-center gap-2">
                        <span
                          className="inline-block h-3 w-3 rounded-full"
                          style={{ backgroundColor: COLORS[idx % COLORS.length] }}
                        />
                        <span className="font-medium">{pm.metodo}</span>
                      </div>
                      <div className="flex items-center gap-4">
                        <span className="text-gray-500">{pm.cantidad} ops</span>
                        <span className="font-semibold">{formatCurrency(pm.monto)}</span>
                        <span className="text-xs text-gray-400">{pm.porcentaje.toFixed(1)}%</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}
        </div>
      )}
    </Layout>
  );
}

function MetricBox({ label, value, color }: { label: string; value: string; color?: string }) {
  return (
    <div className="rounded-lg bg-white p-3 shadow-sm">
      <p className="text-xs font-medium uppercase tracking-wider text-gray-500">{label}</p>
      <p className={`mt-1 text-lg font-bold ${color || 'text-gray-900'}`}>{value}</p>
    </div>
  );
}
