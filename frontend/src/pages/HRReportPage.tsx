import { useEffect, useState } from 'react';
import Layout from '../components/common/Layout';
import api from '../services/api';

interface HRReportResponse {
  resumen: {
    totalEmpleados: number;
    totalNomina: number;
    costoLaboralTotal: number;
    productividadPromedio: number;
    ausentismoPorcentaje: number;
  } | null;
  productividadVendedores: Array<{
    employeeId: number;
    nombre: string;
    cargo: string;
    ventasPeriodo: number;
    transacciones: number;
    ventasPorHora: number;
    comisiones: number;
  }>;
  status: string;
}

function fmt(n: number) {
  return '$' + (n ?? 0).toLocaleString('es-AR', { minimumFractionDigits: 2 });
}

function pct(n: number) {
  return (n ?? 0).toFixed(1) + '%';
}

export default function HRReportPage() {
  const [data, setData] = useState<HRReportResponse | null>(null);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    try {
      const res = await api.get<HRReportResponse>('/hr/report');
      setData(res.data);
    } catch { /* handled */ } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  return (
    <Layout title="Reporte de RRHH">
      {loading && (
        <div className="flex justify-center py-12">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent" />
        </div>
      )}

      {data && data.status === 'ERROR' && (
        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          Error al cargar reporte de RRHH
        </div>
      )}

      {data && data.status === 'OK' && (
        <div className="space-y-6">
          {/* Resumen */}
          {data.resumen && (
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-5">
              <MetricBox label="Empleados" value={String(data.resumen.totalEmpleados)} />
              <MetricBox label="Nómina Total" value={fmt(data.resumen.totalNomina)} />
              <MetricBox label="Costo Laboral" value={fmt(data.resumen.costoLaboralTotal)} />
              <MetricBox label="Productividad" value={fmt(data.resumen.productividadPromedio)} />
              <MetricBox label="Ausentismo" value={pct(data.resumen.ausentismoPorcentaje)} color={data.resumen.ausentismoPorcentaje > 10 ? 'text-red-500' : 'text-green-600'} />
            </div>
          )}

          {/* Productividad vendedores */}
          <div className="rounded-lg bg-surface ">
            <h3 className="px-4 pt-4 text-sm font-semibold uppercase tracking-wider text-ink-faint">
              Productividad por Vendedor
            </h3>
            {data.productividadVendedores.length > 0 ? (
              <table className="min-w-full text-sm">
                <thead>
                  <tr className="border-b bg-surface/50 text-left text-xs uppercase text-ink-faint">
                    <th className="px-4 py-3">Nombre</th>
                    <th className="px-4 py-3">Cargo</th>
                    <th className="px-4 py-3 text-right">Ventas</th>
                    <th className="px-4 py-3 text-right">Transacc.</th>
                    <th className="px-4 py-3 text-right">$/Hora</th>
                    <th className="px-4 py-3 text-right">Comisiones</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {data.productividadVendedores.map((v, idx) => (
                    <tr key={idx} className="hover:bg-surface/50">
                      <td className="px-4 py-2 font-medium text-ink">{v.nombre}</td>
                      <td className="px-4 py-2 text-ink-faint">{v.cargo}</td>
                      <td className="px-4 py-2 text-right font-medium">{fmt(v.ventasPeriodo)}</td>
                      <td className="px-4 py-2 text-right text-ink-soft">{v.transacciones}</td>
                      <td className="px-4 py-2 text-right text-ink-soft">{fmt(v.ventasPorHora)}</td>
                      <td className="px-4 py-2 text-right text-ink-soft">{fmt(v.comisiones)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p className="p-4 text-sm text-ink-faint">Sin datos de productividad</p>
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
    <div className="rounded-lg bg-surface p-4 ">
      <p className="text-xs font-medium uppercase tracking-wider text-ink-faint">{label}</p>
      <p className={`mt-1 text-2xl font-bold ${color || 'text-ink'}`}>{value}</p>
    </div>
  );
}


