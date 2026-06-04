import { useState, useEffect, useRef } from 'react';
import { returnApi, saleApi } from '../../services/api';
import type { SaleResponse, SaleReturnResponse } from '../../types';

interface Props {
  open: boolean;
  onClose: () => void;
  onSuccess?: (ret: SaleReturnResponse) => void;
}

export default function ReturnModal({ open, onClose, onSuccess }: Props) {
  const inputRef = useRef<HTMLInputElement>(null);

  // Step 1: buscar venta
  const [saleId, setSaleId] = useState('');
  const [sale, setSale] = useState<SaleResponse | null>(null);
  const [loadingSale, setLoadingSale] = useState(false);
  const [searchError, setSearchError] = useState('');

  // Step 2: devolución
  const [quantities, setQuantities] = useState<Record<number, number>>({});
  const [motivo, setMotivo] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<SaleReturnResponse | null>(null);
  const [submitError, setSubmitError] = useState('');

  // Reset al abrir
  useEffect(() => {
    if (open) {
      setSaleId('');
      setSale(null);
      setQuantities({});
      setMotivo('');
      setResult(null);
      setSubmitError('');
      setSearchError('');
      setTimeout(() => inputRef.current?.focus(), 100);
    }
  }, [open]);

  if (!open) return null;

  const handleSearch = async () => {
    const id = Number(saleId);
    if (!id) { setSearchError('Ingrese un ID de venta válido'); return; }

    setLoadingSale(true);
    setSearchError('');
    setSale(null);
    try {
      const { data } = await saleApi.getById(id);
      setSale(data);
      // Inicializar quantities con 0 para cada item
      const init: Record<number, number> = {};
      data.items.forEach((item) => { init[item.id] = 0; });
      setQuantities(init);
    } catch {
      setSearchError('Venta no encontrada. Verifique el ID.');
    } finally {
      setLoadingSale(false);
    }
  };

  const hasSelectedItems = Object.values(quantities).some((q) => q > 0);
  const selectedCount = Object.values(quantities).reduce((a, b) => a + b, 0);
  const canSubmit = hasSelectedItems && motivo.trim().length >= 3 && !submitting;

  const handleSubmit = async () => {
    if (!canSubmit || !sale) return;
    setSubmitting(true);
    setSubmitError('');

    const items = Object.entries(quantities)
      .filter(([_, qty]) => qty > 0)
      .map(([saleItemId, cantidad]) => ({
        saleItemId: Number(saleItemId),
        cantidad,
      }));

    try {
      const { data } = await returnApi.create({
        saleId: sale.id,
        motivo: motivo.trim(),
        items,
      });
      setResult(data);
      onSuccess?.(data);
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : undefined;
      setSubmitError(msg || 'Error al procesar la devolución.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !sale) {
      handleSearch();
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
      role="dialog"
      aria-modal="true"
      aria-label="Devolución"
    >
      <div className="w-full max-w-lg rounded-xl bg-white p-6 shadow-xl max-h-[90vh] overflow-y-auto">
        {/* Título */}
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-bold text-gray-800">
            {result ? 'Resultado' : sale ? 'Seleccionar items a devolver' : 'Devolución'}
          </h2>
          <button
            onClick={onClose}
            className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
            aria-label="Cerrar"
          >
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* ── PASO 1: Buscar venta ─────────────────────────── */}
        {!sale && !result && (
          <>
            <p className="mb-3 text-sm text-gray-500">
              Ingrese el ID de la venta para iniciar la devolución.
            </p>
            <div className="flex gap-2">
              <input
                ref={inputRef}
                type="number"
                min={1}
                value={saleId}
                onChange={(e) => setSaleId(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="ID de venta"
                className="flex-1 rounded-lg border border-gray-300 px-3 py-2.5 text-sm focus:border-blue-500 focus:outline-none"
              />
              <button
                onClick={handleSearch}
                disabled={loadingSale || !saleId}
                className="rounded-lg bg-blue-600 px-5 py-2.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
              >
                {loadingSale ? 'Buscando…' : 'Buscar'}
              </button>
            </div>
            {searchError && (
              <p className="mt-2 text-sm text-red-600" role="alert">{searchError}</p>
            )}
          </>
        )}

        {/* ── PASO 2: Seleccionar items ────────────────────── */}
        {sale && !result && (
          <>
            {/* Info de la venta */}
            <div className="mb-4 rounded-lg bg-gray-50 p-3 text-sm text-gray-600">
              <p><strong>Venta #{sale.id}</strong> — {new Date(sale.createdAt).toLocaleDateString('es-CL')}</p>
              <p>Cliente: {sale.client?.name ?? 'Consumidor Final'}</p>
              <p>Total original: <strong>${sale.total.toLocaleString('es-CL')}</strong></p>
            </div>

            {/* Motivo */}
            <div className="mb-4">
              <label htmlFor="return-motivo" className="mb-1 block text-sm font-medium text-gray-700">
                Motivo de la devolución
              </label>
              <input
                id="return-motivo"
                type="text"
                value={motivo}
                onChange={(e) => setMotivo(e.target.value)}
                placeholder="Ej: Producto defectuoso, cambio de talla…"
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
              />
            </div>

            {/* Items de la venta */}
            <div className="mb-4">
              <h3 className="mb-2 text-sm font-medium text-gray-700">Productos</h3>
              <div className="space-y-2 max-h-64 overflow-y-auto">
                {sale.items.map((item) => {
                  const qty = quantities[item.id] ?? 0;
                  return (
                    <div
                      key={item.id}
                      className={`flex items-center gap-3 rounded-lg border p-3 transition-colors ${
                        qty > 0 ? 'border-blue-300 bg-blue-50' : 'border-gray-200'
                      }`}
                    >
                      <div className="flex-1 min-w-0">
                        <p className="truncate text-sm font-medium text-gray-800">{item.productName}</p>
                        <p className="text-xs text-gray-500">
                          ${item.unitPrice.toLocaleString('es-CL')} × {item.quantity} — Subtotal: ${item.subtotal.toLocaleString('es-CL')}
                        </p>
                      </div>
                      <div className="flex items-center gap-1">
                        <button
                          type="button"
                          onClick={() =>
                            setQuantities((prev) => ({
                              ...prev,
                              [item.id]: Math.max(0, (prev[item.id] ?? 0) - 1),
                            }))
                          }
                          disabled={qty === 0}
                          className="flex h-7 w-7 items-center justify-center rounded border text-sm disabled:opacity-30"
                          aria-label="Reducir cantidad"
                        >
                          −
                        </button>
                        <span className="w-8 text-center text-sm font-semibold tabular-nums">{qty}</span>
                        <button
                          type="button"
                          onClick={() =>
                            setQuantities((prev) => ({
                              ...prev,
                              [item.id]: Math.min(item.quantity, (prev[item.id] ?? 0) + 1),
                            }))
                          }
                          disabled={qty >= item.quantity}
                          className="flex h-7 w-7 items-center justify-center rounded border text-sm disabled:opacity-30"
                          aria-label="Aumentar cantidad"
                        >
                          +
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
              {selectedCount > 0 && (
                <p className="mt-2 text-xs text-gray-500">
                  {selectedCount} unidad(es) seleccionada(s) para devolución
                </p>
              )}
            </div>

            {/* Error */}
            {submitError && (
              <p className="mb-3 text-sm text-red-600" role="alert">{submitError}</p>
            )}

            {/* Acciones */}
            <div className="flex gap-3">
              <button
                type="button"
                onClick={() => { setSale(null); setSaleId(''); }}
                className="flex-1 rounded-lg border border-gray-300 py-2.5 text-sm font-medium text-gray-700 hover:bg-gray-50"
              >
                Volver
              </button>
              <button
                type="button"
                onClick={handleSubmit}
                disabled={!canSubmit}
                className="flex-1 rounded-lg bg-orange-600 py-2.5 text-sm font-medium text-white hover:bg-orange-700 disabled:opacity-50"
              >
                {submitting ? 'Procesando…' : `Devolver (${selectedCount} und.)`}
              </button>
            </div>
          </>
        )}

        {/* ── RESULTADO ─────────────────────────────────────── */}
        {result && (
          <div className="text-center">
            <div className="mb-3 text-4xl" aria-hidden="true">
              {result.estado === 'APROBADA' ? '✅' : result.estado === 'RECHAZADA' ? '❌' : '⏳'}
            </div>
            <p className="mb-1 text-lg font-bold text-gray-800">
              {result.estado === 'APROBADA'
                ? 'Devolución aprobada'
                : result.estado === 'RECHAZADA'
                  ? 'Devolución rechazada'
                  : 'Pendiente de aprobación'}
            </p>
            <p className="text-sm text-gray-500">
              Devolución #{result.id} — Venta #{result.saleId}
            </p>
            <p className="mt-1 text-sm text-gray-600">
              Monto: <strong>${result.montoTotal.toLocaleString('es-CL')}</strong>
            </p>
            <p className="mt-1 text-xs text-gray-400">
              Método: {result.metodoDevolucion}
            </p>
            {result.estado === 'PENDIENTE_APROBACION' && (
              <p className="mt-2 text-sm text-amber-600">
                Un administrador o gerente debe aprobar esta devolución.
              </p>
            )}
            <button
              type="button"
              onClick={onClose}
              className="mt-4 rounded-lg bg-blue-600 px-6 py-2.5 text-sm font-medium text-white hover:bg-blue-700"
              autoFocus
            >
              Cerrar
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
