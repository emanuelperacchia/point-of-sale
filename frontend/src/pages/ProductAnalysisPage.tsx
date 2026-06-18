import { useEffect, useState } from 'react';
import Layout from '../components/common/Layout';
import api from '../services/api';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

interface ProductAnalysisResponse {
  clasificacionABC: Array<{
    productId: number;
    productName: string;
    productSku: string;
    categoria: string;
    ventas: number;
    cantidad: number;
    porcentajeAcumulado: number;
    clasificacion: string;
  }>;
  resumen: {
    totalProductos: number;
    productosClaseA: number;
    productosClaseB: number;
    productosClaseC: number;
    ventasClaseA: number;
    ventasClaseB: number;
    ventasClaseC: number;
    porcentajeACobertura: number;
  } | null;
  sinMovimiento: Array<{
    productId: number;
    productName: string;
    productSku: string;
    stockActual: number;
    costoPromedio: number;
    diasSinVenta: number;
  }>;
  status: string;
}

const classColors: Record<string, string> = {
  A: 'bg-green-100 text-green-800 border-green-300',
  B: 'bg-yellow-100 text-yellow-800 border-yellow-300',
  C: 'bg-red-100 text-red-800 border-red-300',
};

const classBarColors: Record<string, string> = {
  A: '#10b981',
  B: '#f59e0b',
  C: '#ef4444',
};

function formatCurrency(n: number) {
  return '$' + (n ?? 0).toLocaleString('es-AR', { minimumFractionDigits: 2 });
}

export default function ProductAnalysisPage() {
  const [data, setData] = useState<ProductAnalysisResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [meses, setMeses] = useState('3');

  const load = async (m: string) => {
    setLoading(true);
    try {
      const res = await api.get<ProductAnalysisResponse>('/analysis/products-abc', {
        params: {
          hasta: new Date().toISOString().split('T')[0],
          meses: m,
          diasSinVenta: 90,
        },
      });
      setData(res.data);
    } catch {
      // handled by interceptor
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load(meses);
  }, []);

  const handleRefresh = () => load(meses);

  // Datos para Pareto
  const paretoData = data?.clasificacionABC?.map((p) => ({
    name: p.productName.length > 20 ? p.productName.substring(0, 18) + '…' : p.productName,
    ventas: p.ventas,
    acumulado: p.porcentajeAcumulado,
    clase: p.clasificacion,
  })) || [];

  return (
    <Layout title="Análisis de Productos (ABC)">
      {/* Controles */}
      <div className="mb-6 flex items-center gap-3">
        <span className="text-sm font-medium text-gray-500">Período:</span>
        {['1', '3', '6', '12'].map((m) => (
          <button
            key={m}
            onClick={() => { setMeses(m); load(m); }}
            className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
              meses === m
                ? 'bg-blue-600 text-white shadow-sm'
                : 'bg-white text-gray-600 hover:bg-gray-100'
            }`}
          >
            {m} {+m === 1 ? 'mes' : 'meses'}
          </button>
        ))}
        <button onClick={handleRefresh} className="ml-2 rounded-md bg-gray-100 px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-200">
          ↻
        </button>
      </div>

      {loading && (
        <div className="flex justify-center py-12">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent" />
        </div>
      )}

      {data && data.status === 'ERROR' && (
        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          Error al cargar análisis de productos
        </div>
      )}

      {data && data.status === 'OK' && (
        <div className="space-y-6">
          {/* Resumen */}
          {data.resumen && (
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-7">
              <Metric label="Total Productos" value={String(data.resumen.totalProductos)} />
              <Metric label="Clase A" value={String(data.resumen.productosClaseA)} color="text-green-600" />
              <Metric label="Clase B" value={String(data.resumen.productosClaseB)} color="text-yellow-600" />
              <Metric label="Clase C" value={String(data.resumen.productosClaseC)} color="text-red-500" />
              <Metric label="Ventas A" value={formatCurrency(data.resumen.ventasClaseA)} />
              <Metric label="Ventas B" value={formatCurrency(data.resumen.ventasClaseB)} />
              <Metric label="% Cobertura A" value={data.resumen.porcentajeACobertura.toFixed(1) + '%'} />
            </div>
          )}

          {/* Gráfico Pareto */}
          {paretoData.length > 0 && (
            <div className="rounded-lg bg-white p-4 shadow-sm">
              <h3 className="mb-3 text-sm font-semibold uppercase tracking-wider text-gray-500">
                Diagrama de Pareto
              </h3>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={paretoData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                  <XAxis
                    dataKey="name"
                    tick={{ fontSize: 10 }}
                    angle={-30}
                    textAnchor="end"
                    height={60}
                  />
                  <YAxis yAxisId="left" tick={{ fontSize: 11 }} tickFormatter={(v: number) => '$' + (v / 1000).toFixed(0) + 'k'} />
                  <YAxis yAxisId="right" orientation="right" tick={{ fontSize: 11 }} domain={[0, 100]} unit="%" />
                  <Tooltip
                    formatter={(value: number, name: string) =>
                      name === 'acumulado' ? value.toFixed(1) + '%' : formatCurrency(value)
                    }
                  />
                  <Bar yAxisId="left" dataKey="ventas" name="Ventas" radius={[3, 3, 0, 0]}>
                    {paretoData.map((entry, idx) => (
                      <rect key={idx} fill={classBarColors[entry.clase] || '#6b7280'} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}

          {/* Tabla ABC */}
          <div className="overflow-x-auto rounded-lg bg-white shadow-sm">
            <table className="min-w-full text-sm">
              <thead>
                <tr className="border-b bg-gray-50 text-left text-xs uppercase tracking-wider text-gray-500">
                  <th className="px-4 py-3">#</th>
                  <th className="px-4 py-3">Producto</th>
                  <th className="px-4 py-3">SKU</th>
                  <th className="px-4 py-3">Categoría</th>
                  <th className="px-4 py-3 text-right">Ventas</th>
                  <th className="px-4 py-3 text-right">Cantidad</th>
                  <th className="px-4 py-3 text-right">% Acum.</th>
                  <th className="px-4 py-3 text-center">Clase</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {data.clasificacionABC.map((p, idx) => (
                  <tr key={p.productId} className="hover:bg-gray-50">
                    <td className="px-4 py-2 text-gray-400">{idx + 1}</td>
                    <td className="max-w-[200px] truncate px-4 py-2 font-medium text-gray-800">
                      {p.productName}
                    </td>
                    <td className="px-4 py-2 text-gray-500">{p.productSku}</td>
                    <td className="px-4 py-2 text-gray-500">{p.categoria}</td>
                    <td className="px-4 py-2 text-right font-medium">{formatCurrency(p.ventas)}</td>
                    <td className="px-4 py-2 text-right text-gray-500">{p.cantidad}</td>
                    <td className="px-4 py-2 text-right text-gray-500">{p.porcentajeAcumulado.toFixed(1)}%</td>
                    <td className="px-4 py-2 text-center">
                      <span className={`inline-block rounded-md border px-2 py-0.5 text-xs font-bold ${classColors[p.clasificacion] || ''}`}>
                        {p.clasificacion}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Sin movimiento */}
          {data.sinMovimiento.length > 0 && (
            <div className="rounded-lg bg-white p-4 shadow-sm">
              <h3 className="mb-3 text-sm font-semibold uppercase tracking-wider text-gray-500">
                Productos sin Movimiento ({data.sinMovimiento.length})
              </h3>
              <div className="flex flex-wrap gap-2">
                {data.sinMovimiento.map((p) => (
                  <span
                    key={p.productId}
                    className="rounded-full bg-gray-100 px-3 py-1 text-xs text-gray-600"
                    title={p.productName}
                  >
                    {p.productName}
                  </span>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </Layout>
  );
}

function Metric({ label, value, color }: { label: string; value: string; color?: string }) {
  return (
    <div className="rounded-lg bg-white p-3 shadow-sm">
      <p className="text-xs font-medium uppercase tracking-wider text-gray-500">{label}</p>
      <p className={`mt-1 text-lg font-bold ${color || 'text-gray-900'}`}>{value}</p>
    </div>
  );
}
