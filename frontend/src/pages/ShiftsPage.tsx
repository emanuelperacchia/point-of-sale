import { useState, useEffect } from 'react';
import Layout from '../components/common/Layout';
import { shiftApi } from '../services/api';
import type { ShiftResponse, ShiftStatus } from '../types';

const STATUS_LABELS: Record<ShiftStatus, string> = {
  ABIERTO: 'Abierto',
  CERRADO: 'Cerrado',
};

export default function ShiftsPage() {
  const [shifts, setShifts] = useState<ShiftResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [viewing, setViewing] = useState(false);
  const [currentShift, setCurrentShift] = useState<ShiftResponse | null>(null);
  const [report, setReport] = useState<ShiftReportResponse | null>(null);

  useEffect(() => {
    loadShifts();
  }, []);

  const loadShifts = async () => {
    setLoading(true);
    setError('');
    try {
      const { data } = await shiftApi.findByFilters();
      setShifts(data);
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Error al cargar turnos';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleViewShift = async (shift: ShiftResponse) => {
    setViewing(true);
    setCurrentShift(shift);
    try {
      const { data } = await shiftApi.getReport(shift.id);
      setReport(data);
    } catch {
      setReport(null);
    }
  };

  if (loading) {
    return (
      <Layout title="Cierre de Caja">
        <div className="flex h-full items-center justify-center text-ink-faint">
          Cargando turnos…
        </div>
      </Layout>
    );
  }

  return (
    <Layout title="Turnos y Cierre de Caja">
      <div className="space-y-6">
        {/* Encabezado */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-ink">Turnos de Caja</h1>
            <p className="text-sm text-ink-faint">
              Gestiona la apertura y cierre de turnos de caja por sucursal.
            </p>
          </div>
        </div>

        {error && (
          <div className="rounded-md border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger">
            {error}
          </div>
        )}

        {/* Tabla de turnos */}
        <div className="rounded-xl border border-surface-raised bg-surface/80 ">
          <table className="w-full text-sm text-left border-collapse">
            <thead>
              <tr className="border-b border-surface-raised bg-surface-raised/50 text-ink-soft">
                <th className="p-4 font-medium text-ink">N° Turno</th>
                <th className="p-4 font-medium text-ink">Cajero</th>
                <th className="p-4 font-medium text-ink-soft">Fecha Apertura</th>
                <th className="p-4 font-medium text-ink-soft text-right">Monto Apertura</th>
                <th className="p-4 font-medium text-ink-soft text-center">Estado</th>
                <th className="p-4 font-medium text-ink-soft text-right">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {shifts.map((s) => (
                <tr key={s.id} className="border-b border-surface-raised/50 hover:bg-surface-raised/30 transition-colors">
                  <td className="p-4 text-ink font-medium">#{s.id}</td>
                  <td className="p-4 text-ink-soft">{s.cajeroNombre}</td>
                  <td className="p-4 text-ink-soft">
                    {new Date(s.fechaApertura).toLocaleDateString('es-AR')}
                  </td>
                  <td className="p-4 text-right text-ink-soft font-mono">
                    ${s.montoApertura.toFixed(2)}
                  </td>
                  <td className="p-4 text-center">
                    <span
                      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${
                        s.estado === 'ABIERTO'
                          ? 'bg-success/10 text-success border border-success/20'
                          : 'bg-tertiary/10 text-tertiary border border-tertiary/20'
                      }`}
                    >
                      <span
                        className={`w-1.5 h-1.5 rounded-full ${
                          s.estado === 'ABIERTO' ? 'bg-success' : 'bg-tertiary'
                        }`}
                        aria-hidden="true"
                      />
                      {STATUS_LABELS[s.estado]}
                    </span>
                  </td>
                  <td className="p-4 text-right">
                    <button
                      onClick={() => handleViewShift(s)}
                      className="text-accent hover:text-primary-soft transition-colors"
                      title="Ver reporte"
                    >
                      Ver reporte
                    </button>
                  </td>
                </tr>
              ))}
              {shifts.length === 0 && (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-ink-faint">
                    No hay turnos registrados todavía.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Modal reporte de turno */}
        {viewing && currentShift && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <div
              className="absolute inset-0 bg-black/40 backdrop-blur-sm"
              onClick={() => setViewing(false)}
              aria-hidden="true"
            />
            <div className="relative w-full max-w-lg rounded-xl bg-surface p-6  border border-surface-raised">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-bold text-ink">Reporte de Turno #{currentShift.id}</h2>
                <button
                  onClick={() => setViewing(false)}
                  className="rounded-md p-2 text-ink-faint hover:text-ink hover:bg-surface-raised transition-colors"
                  aria-label="Cerrar"
                >
                  <span aria-hidden="true">✕</span>
                </button>
              </div>

              <div className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="mb-1 block text-sm font-medium text-ink-soft">
                      Cajero
                    </label>
                    <p className="text-ink">{currentShift.cajeroNombre}</p>
                  </div>
                  <div>
                    <label className="mb-1 block text-sm font-medium text-ink-soft">
                      Estado
                    </label>
                    <p className="text-ink">{STATUS_LABELS[currentShift.estado]}</p>
                  </div>
                  <div>
                    <label className="mb-1 block text-sm font-medium text-ink-soft">
                      Fecha Apertura
                    </label>
                    <p className="text-ink">
                      {new Date(currentShift.fechaApertura).toLocaleString('es-AR')}
                    </p>
                  </div>
                  <div>
                    <label className="mb-1 block text-sm font-medium text-ink-soft">
                      Monto Apertura
                    </label>
                    <p className="text-ink">${currentShift.montoApertura.toFixed(2)}</p>
                  </div>
                  {currentShift.fechaCierre && (
                    <div>
                      <label className="mb-1 block text-sm font-medium text-ink-soft">
                        Fecha Cierre
                      </label>
                      <p className="text-ink">
                        {new Date(currentShift.fechaCierre).toLocaleString('es-AR')}
                      </p>
                    </div>
                  )}
                  {currentShift.montoCierre !== null && (
                    <div>
                      <label className="mb-1 block text-sm font-medium text-ink-soft">
                        Monto Cierre
                      </label>
                      <p className="text-ink">${currentShift.montoCierre.toFixed(2)}</p>
                    </div>
                  )}
                  {currentShift.diferencia !== null && (
                    <div>
                      <label className="mb-1 block text-sm font-medium text-ink-soft">
                        Diferencia
                      </label>
                      <p className={`text-ink ${currentShift.diferencia >= 0 ? 'text-success' : 'text-danger'}`}>
                        {currentShift.diferencia >= 0 ? '+' : ''}$${currentShift.diferencia.toFixed(2)}
                      </p>
                    </div>
                  )}
                </div>

                {report && (
                  <div className="rounded-lg border border-surface-raised bg-surface/50 p-4">
                    <h3 className="mb-2 text-sm font-semibold text-ink">Resumen del Turno</h3>
                    <div className="grid grid-cols-2 gap-4 text-sm">
                      <div>
                        <p className="text-xs text-ink-faint uppercase">Total Ventas Efectivo</p>
                        <p className="text-ink">${report.totalVentasEfectivo.toFixed(2)}</p>
                      </div>
                      <div>
                        <p className="text-xs text-ink-faint uppercase">Total Ingresos</p>
                        <p className="text-ink">${report.totalIngresos.toFixed(2)}</p>
                      </div>
                      <div>
                        <p className="text-xs text-ink-faint uppercase">Total Retiros</p>
                        <p className="text-ink">${report.totalRetiros.toFixed(2)}</p>
                      </div>
                      <div>
                        <p className="text-xs text-ink-faint uppercase">Monto Esperado</p>
                        <p className="text-ink">${report.montoEsperado.toFixed(2)}</p>
                      </div>
                    </div>

                    <div className="mt-4">
                      <label className="mb-1 block text-xs font-medium text-ink-faint uppercase">
                        Ventas por Método de Pago
                      </label>
                      <ul className="space-y-1">
                        {Object.entries(report.ventasPorMetodoPago).map(([method, amount]) => (
                          <li key={method} className="flex justify-between text-sm">
                            <span className="text-ink-soft">{method}</span>
                            <span className="text-ink">${amount.toFixed(2)}</span>
                          </li>
                        ))}
                      </ul>
                    </div>

                    <div className="mt-4">
                      <label className="mb-1 block text-xs font-medium text-ink-faint uppercase">
                        Movimientos
                      </label>
                      <ul className="space-y-1 max-h-40 overflow-y-auto">
                        {report.movimientos.map((m) => (
                          <li key={m.id} className="flex justify-between text-sm text-ink-soft border-b border-surface-raised/50 pb-1">
                            <span>{m.tipo}</span>
                            <span>${m.monto.toFixed(2)}</span>
                          </li>
                        ))}
                        {report.movimientos.length === 0 && (
                          <li className="text-xs text-ink-faint">Sin movimientos</li>
                        )}
                      </ul>
                    </div>
                  </div>
                )}
              </div>

              <div className="flex justify-end pt-4">
                <button
                  onClick={() => setViewing(false)}
                  className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-ink-on-dark transition-colors hover:bg-primary-soft"
                >
                  Cerrar
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
}

