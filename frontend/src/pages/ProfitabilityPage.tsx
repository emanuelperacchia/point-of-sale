import { useEffect, useState } from 'react';
import Layout from '../components/common/Layout';
import api from '../services/api';

interface ProfitabilityResponse {
  margenBruto: number;
  margenNeto: number;
  margenBrutoPorcentaje: number;
  margenNetoPorcentaje: number;
  puntoEquilibrio: number;
  porProducto: Array<{
    productId: number;
    productName: string;
    ingresos: number;
    costo: number;
    margen: number;
    margenPorcentaje: number;
  }>;
  status: string;
}

function fmt(n: number) {
  return '$' + (n ?? 0).toLocaleString('es-AR', { minimumFractionDigits: 2 });
}

function pct(n: number) {
  return (n ?? 0).toFixed(1) + '%';
}

export default function ProfitabilityPage() {
  const [data, setData] = useState<ProfitabilityResponse | null>(null);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    try {
      const res = await api.get<ProfitabilityResponse>('/analysis/profitability');
      setData(res.data);
    } catch { /* handled */ } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  return (
    <Layout title="Análisis de Rentabilidad">
      {loading && (
        <div className="flex justify-center py-12">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent" />
        </div>
      )}

      {data && data.status === 'ERROR' && (
        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          Error al cargar rentabilidad
        </div>
      )}

      {data && data.status === 'OK' && (
        <div className="space-y-6">
          {/* KPIs */}
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <MetricBox label="Margen Bruto" value={fmt(data.margenBruto)} desc={pct(data.margenBrutoPorcentaje)} color="text-green-600" />
            <MetricBox label="Margen Neto" value={fmt(data.margenNeto)} desc={pct(data.margenNetoPorcentaje)} color="text-blue-600" />
            <MetricBox label="Punto Equilibrio" value={fmt(data.puntoEquilibrio)} desc="ventas mínimas" color="text-orange-600" />
            <MetricBox label="Diferencia" value={fmt(data.margenBruto - data.margenNeto)} desc="gastos + impuestos" color="text-ink-soft" />
          </div>

          {/* Por producto */}
          <div className="rounded-lg bg-surface ">
            <h3 className="px-4 pt-4 text-sm font-semibold uppercase tracking-wider text-ink-faint">
              Rentabilidad por Producto
            </h3>
            {data.porProducto.length > 0 ? (
              <table className="min-w-full text-sm">
                <thead>
                  <tr className="border-b bg-surface/50 text-left text-xs uppercase text-ink-faint">
                    <th className="px-4 py-3">Producto</th>
                    <th className="px-4 py-3 text-right">Ingresos</th>
                    <th className="px-4 py-3 text-right">Costo</th>
                    <th className="px-4 py-3 text-right">Margen</th>
                    <th className="px-4 py-3 text-right">%</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {data.porProducto.map((p, idx) => (
                    <tr key={idx} className="hover:bg-surface/50">
                      <td className="px-4 py-2 font-medium text-ink">{p.productName}</td>
                      <td className="px-4 py-2 text-right">{fmt(p.ingresos)}</td>
                      <td className="px-4 py-2 text-right">{fmt(p.costo)}</td>
                      <td className={`px-4 py-2 text-right font-medium ${p.margen >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                        {fmt(p.margen)}
                      </td>
                      <td className={`px-4 py-2 text-right font-medium ${p.margenPorcentaje >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                        {pct(p.margenPorcentaje)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p className="p-4 text-sm text-ink-faint">Sin datos de producto</p>
            )}
          </div>

          <div className="flex justify-end">
            <button onClick={load} className="rounded-md bg-blue-600 px-4 py-2 text-sm text-white hover:bg-blue-700">
              Actualizar
            </button>
          </div>
        </div>
      )}
    </Layout>
  );
}

function MetricBox({ label, value, desc, color }: { label: string; value: string; desc?: string; color?: string }) {
  return (
    <div className="rounded-lg bg-surface p-4 ">
      <p className="text-xs font-medium uppercase tracking-wider text-ink-faint">{label}</p>
      <p className={`mt-1 text-2xl font-bold ${color || 'text-ink'}`}>{value}</p>
      {desc && <p className="mt-0.5 text-xs text-ink-faint">{desc}</p>}
    </div>
  );
}


