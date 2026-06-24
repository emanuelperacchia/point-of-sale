import { useEffect, useState } from 'react';
import Layout from '../components/common/Layout';
import api from '../services/api';

interface ConsolidatedReport {
  totales: {
    ventas: number;
    costoVentas: number;
    margenBruto: number;
    margenPorcentaje: number;
    gastos: number;
    rentabilidadNeta: number;
    ordenesProduccion: number;
    sucursalesActivas: number;
  } | null;
  porSucursal: Array<{
    branchId: number;
    branchName: string;
    ventas: number;
    ticketPromedio: number;
    margenBruto: number;
    participacionPorcentaje: number;
    transacciones: number;
    ausentismo: number;
  }>;
  transferencias: {
    totalTransferencias: number;
    montoTotalTransferido: number;
    sucursalMasEnvia: number | null;
    sucursalMasRecibe: number | null;
  } | null;
  status: string;
}

function fmt(n: number) { return '$' + (n ?? 0).toLocaleString('es-AR', { minimumFractionDigits: 2 }); }
function pct(n: number) { return (n ?? 0).toFixed(1) + '%'; }

export default function ConsolidatedDashboardPage() {
  const [data, setData] = useState<ConsolidatedReport | null>(null);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    try {
      const res = await api.get<ConsolidatedReport>('/reports/consolidated');
      setData(res.data);
    } catch { /* */ } finally { setLoading(false); }
  };

  useEffect(() => { load(); }, []);

  return (
    <Layout title="Dashboard Corporativo (Consolidado)">
      {loading && (
        <div className="flex justify-center py-12">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent" />
        </div>
      )}

      {data && data.status === 'ERROR' && (
        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          Error al cargar reporte consolidado
        </div>
      )}

      {data && data.status === 'OK' && data.totales && (
        <div className="space-y-6">
          {/* Totales Globales */}
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <KpiBox label="Ventas Globales" value={fmt(data.totales.ventas)} />
            <KpiBox label="Margen Bruto" value={fmt(data.totales.margenBruto)} color="text-green-600" />
            <KpiBox label="Gastos" value={fmt(data.totales.gastos)} color="text-red-500" />
            <KpiBox label="Rentabilidad Neta" value={fmt(data.totales.rentabilidadNeta)}
              color={data.totales.rentabilidadNeta >= 0 ? 'text-green-600' : 'text-red-500'} />
          </div>

          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <KpiBox label="Sucursales" value={String(data.totales.sucursalesActivas)} />
            <KpiBox label="Órdenes Prod." value={String(data.totales.ordenesProduccion)} />
            <KpiBox label="Costo Ventas" value={fmt(data.totales.costoVentas)} />
            <KpiBox label="Margen %" value={pct(data.totales.margenPorcentaje)} />
          </div>

          {/* Por sucursal */}
          <div className="rounded-lg bg-white shadow-sm">
            <h3 className="px-4 pt-4 text-sm font-semibold uppercase tracking-wider text-gray-500">
              Comparativa por Sucursal
            </h3>
            <table className="min-w-full text-sm">
              <thead>
                <tr className="border-b bg-gray-50 text-left text-xs uppercase text-gray-500">
                  <th className="px-4 py-3">Sucursal</th>
                  <th className="px-4 py-3 text-right">Ventas</th>
                  <th className="px-4 py-3 text-right">Particip.</th>
                  <th className="px-4 py-3 text-right">Ticket Prom.</th>
                  <th className="px-4 py-3 text-right">Transacc.</th>
                  <th className="px-4 py-3 text-right">Margen</th>
                  <th className="px-4 py-3 text-right">Ausentismo</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {data.porSucursal.map((s) => (
                  <tr key={s.branchId} className="hover:bg-gray-50">
                    <td className="px-4 py-2 font-medium text-gray-800">{s.branchName}</td>
                    <td className="px-4 py-2 text-right">{fmt(s.ventas)}</td>
                    <td className="px-4 py-2 text-right">{pct(s.participacionPorcentaje)}</td>
                    <td className="px-4 py-2 text-right">{fmt(s.ticketPromedio)}</td>
                    <td className="px-4 py-2 text-right text-gray-600">{s.transacciones}</td>
                    <td className="px-4 py-2 text-right text-green-600">{fmt(s.margenBruto)}</td>
                    <td className="px-4 py-2 text-right">{pct(s.ausentismo)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Transferencias */}
          {data.transferencias && (
            <div className="rounded-lg bg-white p-4 shadow-sm">
              <h3 className="text-sm font-semibold uppercase tracking-wider text-gray-500">
                Transferencias del Período
              </h3>
              <div className="mt-2 grid grid-cols-2 gap-4 sm:grid-cols-4">
                <KpiBox label="Total" value={String(data.transferencias.totalTransferencias)} />
                <KpiBox label="Monto Total" value={fmt(data.transferencias.montoTotalTransferido)} />
                <KpiBox label="Más Envíos" value={data.transferencias.sucursalMasEnvia != null ?
                  `#${data.transferencias.sucursalMasEnvia}` : 'N/A'} />
                <KpiBox label="Más Recibe" value={data.transferencias.sucursalMasRecibe != null ?
                  `#${data.transferencias.sucursalMasRecibe}` : 'N/A'} />
              </div>
            </div>
          )}

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

function KpiBox({ label, value, color }: { label: string; value: string; color?: string }) {
  return (
    <div className="rounded-lg bg-white p-4 shadow-sm">
      <p className="text-xs font-medium uppercase tracking-wider text-gray-500">{label}</p>
      <p className={`mt-1 text-2xl font-bold ${color || 'text-gray-900'}`}>{value}</p>
    </div>
  );
}
