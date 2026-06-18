import { useEffect, useState } from 'react';
import Layout from '../components/common/Layout';
import api from '../services/api';

interface InventoryReportResponse {
  valorizacion: {
    valorTotal: number;
    totalProducts: number;
    bajoStock: number;
    sinStock: number;
  } | null;
  movimientosRecientes: Array<{
    productId: number;
    productName: string;
    tipo: string;
    cantidad: number;
    fecha: string;
    referencia: string | null;
  }>;
  status: string;
}

function formatCurrency(n: number) {
  return '$' + (n ?? 0).toLocaleString('es-AR', { minimumFractionDigits: 2 });
}

export default function InventoryReportPage() {
  const [data, setData] = useState<InventoryReportResponse | null>(null);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    try {
      const res = await api.get<InventoryReportResponse>('/analysis/inventory');
      setData(res.data);
    } catch { /* handled */ } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  return (
    <Layout title="Reporte de Inventario">
      {loading && (
        <div className="flex justify-center py-12">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent" />
        </div>
      )}

      {data && data.status === 'ERROR' && (
        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          Error al cargar reporte de inventario
        </div>
      )}

      {data && data.status === 'OK' && (
        <div className="space-y-6">
          {/* Valorización */}
          {data.valorizacion && (
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
              <MetricBox label="Valor Inventario" value={formatCurrency(data.valorizacion.valorTotal)} />
              <MetricBox label="Productos Activos" value={String(data.valorizacion.totalProducts)} />
              <MetricBox label="Stock Crítico" value={String(data.valorizacion.bajoStock)} color="text-red-500" />
              <MetricBox label="Sin Stock" value={String(data.valorizacion.sinStock)} color="text-yellow-600" />
            </div>
          )}

          {/* Movimientos recientes */}
          <div className="rounded-lg bg-white shadow-sm">
            <h3 className="px-4 pt-4 text-sm font-semibold uppercase tracking-wider text-gray-500">
              Movimientos Recientes
            </h3>
            {data.movimientosRecientes.length > 0 ? (
              <table className="min-w-full text-sm">
                <thead>
                  <tr className="border-b bg-gray-50 text-left text-xs uppercase text-gray-500">
                    <th className="px-4 py-3">Producto</th>
                    <th className="px-4 py-3">Tipo</th>
                    <th className="px-4 py-3 text-right">Cantidad</th>
                    <th className="px-4 py-3">Referencia</th>
                    <th className="px-4 py-3">Fecha</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {data.movimientosRecientes.map((m, idx) => (
                    <tr key={idx} className="hover:bg-gray-50">
                      <td className="px-4 py-2 font-medium text-gray-800">{m.productName}</td>
                      <td className="px-4 py-2">
                        <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                          m.tipo.startsWith('ENTRADA') ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                        }`}>
                          {m.tipo.replace(/_/g, ' ')}
                        </span>
                      </td>
                      <td className="px-4 py-2 text-right font-medium">{m.cantidad}</td>
                      <td className="px-4 py-2 text-gray-500">{m.referencia || '—'}</td>
                      <td className="px-4 py-2 text-gray-500">{m.fecha}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p className="p-4 text-sm text-gray-400">Sin movimientos recientes</p>
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

function MetricBox({ label, value, color }: { label: string; value: string; color?: string }) {
  return (
    <div className="rounded-lg bg-white p-4 shadow-sm">
      <p className="text-xs font-medium uppercase tracking-wider text-gray-500">{label}</p>
      <p className={`mt-1 text-2xl font-bold ${color || 'text-gray-900'}`}>{value}</p>
    </div>
  );
}
