import { useState, useEffect, useCallback, startTransition } from 'react';
import { shiftApi } from '../../services/api';
import type { ShiftResponse, ShiftReportResponse } from '../../types';

/**
 * Panel de control de turno de caja.
 *
 * - Sin turno activo → muestra botón "Abrir turno"
 * - Turno activo → muestra info, botón "Cerrar turno" y formulario de movimientos
 */
export default function ShiftPanel() {
  const [shift, setShift] = useState<ShiftResponse | null>(null);
  const [report, setReport] = useState<ShiftReportResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  // Formulario de apertura
  const [montoApertura, setMontoApertura] = useState('');

  // Formulario de cierre
  const [montoCierre, setMontoCierre] = useState('');
  const [showCloseForm, setShowCloseForm] = useState(false);

  // Formulario de movimiento
  const [movTipo, setMovTipo] = useState<'RETIRO' | 'INGRESO'>('INGRESO');
  const [movMonto, setMovMonto] = useState('');
  const [movMotivo, setMovMotivo] = useState('');
  const [showMovForm, setShowMovForm] = useState(false);

  // ── Cargar turno activo al montar ─────────────
  const loadActiveShift = useCallback(async () => {
    try {
      setLoading(true);
      const { data } = await shiftApi.findByFilters(undefined, 'ABIERTO' as const);
      if (data.length > 0) {
        setShift(data[0]);
      }
    } catch {
      // No hay turno activo — es normal
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    startTransition(() => { loadActiveShift(); });
  }, [loadActiveShift]);

  // ── Abrir turno ────────────────────────────────
  const handleOpen = async () => {
    const monto = Number(montoApertura);
    if (monto <= 0) {
      setError('El monto de apertura debe ser mayor a 0');
      return;
    }
    try {
      setLoading(true);
      setError('');
      const { data } = await shiftApi.open(1, monto);
      setShift(data);
      setMontoApertura('');
      setMessage('Turno abierto correctamente');
    } catch (err: unknown) {
      setError(
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message ||
            'Error al abrir turno'
          : 'Error al abrir turno'
      );
    } finally {
      setLoading(false);
    }
  };

  // ── Cerrar turno ──────────────────────────────
  const handleClose = async () => {
    if (!shift) return;
    const monto = Number(montoCierre);
    if (monto < 0) {
      setError('El monto de cierre no puede ser negativo');
      return;
    }
    try {
      setLoading(true);
      setError('');
      const { data } = await shiftApi.close(shift.id, monto);
      setShift(data);
      setMontoCierre('');
      setShowCloseForm(false);
      setMessage(`Turno cerrado. Diferencia: $${data.diferencia?.toLocaleString('es-CL') ?? 0}`);
    } catch (err: unknown) {
      setError(
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message ||
            'Error al cerrar turno'
          : 'Error al cerrar turno'
      );
    } finally {
      setLoading(false);
    }
  };

  // ── Movimiento manual ─────────────────────────
  const handleMovement = async () => {
    if (!shift) return;
    const monto = Number(movMonto);
    if (monto <= 0) {
      setError('El monto debe ser mayor a 0');
      return;
    }
    if (!movMotivo.trim()) {
      setError('El motivo es obligatorio');
      return;
    }
    try {
      setLoading(true);
      setError('');
      await shiftApi.addMovement(shift.id, movTipo, monto, movMotivo.trim());
      setMovMonto('');
      setMovMotivo('');
      setShowMovForm(false);
      setMessage('Movimiento registrado');
    } catch (err: unknown) {
      setError(
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message ||
            'Error al registrar movimiento'
          : 'Error al registrar movimiento'
      );
    } finally {
      setLoading(false);
    }
  };

  // ── Cargar reporte ────────────────────────────
  const loadReport = useCallback(async () => {
    if (!shift) return;
    try {
      const { data } = await shiftApi.getReport(shift.id);
      setReport(data);
    } catch {
      // silencioso
    }
  }, [shift]);

  useEffect(() => {
    startTransition(() => {
      if (shift?.estado === 'CERRADO') {
        loadReport();
      } else {
        setReport(null);
      }
    });
  }, [shift, loadReport]);

  // ── Render ────────────────────────────────────
  return (
    <section
      className="rounded-lg bg-white p-4 shadow-sm"
      aria-label="Panel de turno"
    >
      <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-gray-500">
        Turno de caja
      </h2>

      {/* Estado del turno */}
      {shift ? (
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs text-gray-500">Estado</span>
            <span
              className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${
                shift.estado === 'ABIERTO'
                  ? 'bg-green-100 text-green-700'
                  : 'bg-gray-100 text-gray-600'
              }`}
            >
              {shift.estado === 'ABIERTO' ? 'Abierto' : 'Cerrado'}
            </span>
          </div>

          <div className="flex items-center justify-between">
            <span className="text-xs text-gray-500">Apertura</span>
            <span className="text-sm font-medium">
              ${shift.montoApertura.toLocaleString('es-CL')}
            </span>
          </div>

          {shift.estado === 'CERRADO' && shift.diferencia != null && (
            <div className="flex items-center justify-between">
              <span className="text-xs text-gray-500">Diferencia</span>
              <span
                className={`text-sm font-medium ${
                  shift.diferencia >= 0 ? 'text-green-600' : 'text-red-600'
                }`}
              >
                ${shift.diferencia.toLocaleString('es-CL')}
              </span>
            </div>
          )}

          {shift.estado === 'CERRADO' && report && (
            <div className="space-y-1 border-t pt-2 text-xs text-gray-500">
              <p>Ventas efectivo: ${report.totalVentasEfectivo.toLocaleString('es-CL')}</p>
              <p>Ingresos: ${report.totalIngresos.toLocaleString('es-CL')}</p>
              <p>Retiros: ${report.totalRetiros.toLocaleString('es-CL')}</p>
              <p className="font-medium text-gray-700">
                Esperado: ${report.montoEsperado.toLocaleString('es-CL')}
              </p>
            </div>
          )}

          {/* Acciones para turno ABIERTO */}
          {shift.estado === 'ABIERTO' && (
            <div className="space-y-2 border-t pt-2">
              {/* Cerrar turno */}
              {!showCloseForm ? (
                <button
                  onClick={() => setShowCloseForm(true)}
                  className="w-full rounded bg-orange-500 px-3 py-1.5 text-xs font-medium text-white hover:bg-orange-600"
                >
                  Cerrar turno
                </button>
              ) : (
                <div className="space-y-2">
                  <input
                    type="number"
                    value={montoCierre}
                    onChange={(e) => setMontoCierre(e.target.value)}
                    placeholder="Monto de cierre"
                    className="w-full rounded border px-2 py-1 text-sm"
                    aria-label="Monto de cierre declarado"
                  />
                  <div className="flex gap-2">
                    <button
                      onClick={handleClose}
                      disabled={loading || !montoCierre}
                      className="flex-1 rounded bg-orange-500 px-3 py-1.5 text-xs font-medium text-white hover:bg-orange-600 disabled:opacity-50"
                    >
                      {loading ? 'Cerrando…' : 'Confirmar cierre'}
                    </button>
                    <button
                      onClick={() => setShowCloseForm(false)}
                      className="rounded bg-gray-200 px-3 py-1.5 text-xs text-gray-600 hover:bg-gray-300"
                    >
                      Cancelar
                    </button>
                  </div>
                </div>
              )}

              {/* Movimiento manual */}
              {!showMovForm ? (
                <button
                  onClick={() => setShowMovForm(true)}
                  className="w-full rounded bg-blue-500 px-3 py-1.5 text-xs font-medium text-white hover:bg-blue-600"
                >
                  + Movimiento
                </button>
              ) : (
                <div className="space-y-2 rounded border p-2">
                  <select
                    value={movTipo}
                    onChange={(e) => setMovTipo(e.target.value as 'RETIRO' | 'INGRESO')}
                    className="w-full rounded border px-2 py-1 text-sm"
                    aria-label="Tipo de movimiento"
                  >
                    <option value="INGRESO">Ingreso</option>
                    <option value="RETIRO">Retiro</option>
                  </select>
                  <input
                    type="number"
                    value={movMonto}
                    onChange={(e) => setMovMonto(e.target.value)}
                    placeholder="Monto"
                    className="w-full rounded border px-2 py-1 text-sm"
                    aria-label="Monto del movimiento"
                  />
                  <input
                    type="text"
                    value={movMotivo}
                    onChange={(e) => setMovMotivo(e.target.value)}
                    placeholder="Motivo"
                    className="w-full rounded border px-2 py-1 text-sm"
                    aria-label="Motivo del movimiento"
                  />
                  <div className="flex gap-2">
                    <button
                      onClick={handleMovement}
                      disabled={loading || !movMonto || !movMotivo.trim()}
                      className="flex-1 rounded bg-blue-500 px-3 py-1.5 text-xs font-medium text-white hover:bg-blue-600 disabled:opacity-50"
                    >
                      {loading ? 'Guardando…' : 'Guardar'}
                    </button>
                    <button
                      onClick={() => setShowMovForm(false)}
                      className="rounded bg-gray-200 px-3 py-1.5 text-xs text-gray-600 hover:bg-gray-300"
                    >
                      Cancelar
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      ) : (
        /* Sin turno activo — formulario de apertura */
        <div className="space-y-3">
          <p className="text-xs text-gray-400">No hay turno activo</p>
          <input
            type="number"
            value={montoApertura}
            onChange={(e) => setMontoApertura(e.target.value)}
            placeholder="Monto inicial en caja"
            className="w-full rounded border px-2 py-1.5 text-sm"
            aria-label="Monto de apertura del turno"
          />
          <button
            onClick={handleOpen}
            disabled={loading || !montoApertura}
            className="w-full rounded bg-green-600 px-3 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50"
          >
            {loading ? 'Abriendo…' : 'Abrir turno'}
          </button>
        </div>
      )}

      {/* Feedback */}
      {error && (
        <div className="mt-2 rounded border border-red-300 bg-red-50 px-2 py-1.5 text-xs text-red-700" role="alert">
          {error}
        </div>
      )}
      {message && (
        <div className="mt-2 rounded border border-green-300 bg-green-50 px-2 py-1.5 text-xs text-green-700" role="status">
          {message}
        </div>
      )}
    </section>
  );
}
