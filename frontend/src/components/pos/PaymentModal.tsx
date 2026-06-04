import { useState } from 'react';
import type { PaymentMethod } from '../../types';

interface Props {
  open: boolean;
  total: number;
  onConfirm: (payment: { method: PaymentMethod; amount: number; reference?: string }) => void;
  onCancel: () => void;
}

const METHODS: { value: PaymentMethod; label: string; icon: string }[] = [
  { value: 'CASH', label: 'Efectivo', icon: '💰' },
  { value: 'DEBIT_CARD', label: 'Débito', icon: '💳' },
  { value: 'CREDIT_CARD', label: 'Crédito', icon: '💳' },
  { value: 'TRANSFER', label: 'Transferencia', icon: '🏦' },
];

export default function PaymentModal({ open, total, onConfirm, onCancel }: Props) {
  const [method, setMethod] = useState<PaymentMethod>('CASH');
  const [received, setReceived] = useState(total);
  const [reference, setReference] = useState('');
  const [error, setError] = useState('');

  if (!open) return null;

  const isCash = method === 'CASH';
  const change = isCash ? received - total : 0;
  const canSubmit = isCash ? received >= total : true;

  const handleSubmit = () => {
    if (!canSubmit) {
      setError(isCash ? 'El monto recibido debe ser mayor o igual al total' : '');
      return;
    }
    setError('');
    onConfirm({
      method,
      amount: isCash ? received : total,
      reference: reference.trim() || undefined,
    });
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
      onClick={(e) => { if (e.target === e.currentTarget) onCancel(); }}
      role="dialog"
      aria-modal="true"
      aria-label="Cobrar venta"
    >
      <div className="w-full max-w-md rounded-xl bg-white p-6 shadow-xl">
        {/* Título */}
        <h2 className="mb-4 text-lg font-bold text-gray-800">Cobrar</h2>

        {/* Total a cobrar */}
        <div className="mb-4 rounded-lg bg-blue-50 p-3 text-center">
          <span className="text-sm text-gray-600">Total</span>
          <p className="text-2xl font-bold text-blue-700">
            ${total.toLocaleString('es-CL')}
          </p>
        </div>

        {/* Método de pago */}
        <fieldset className="mb-4">
          <legend className="mb-2 text-sm font-medium text-gray-700">
            Método de pago
          </legend>
          <div className="grid grid-cols-2 gap-2">
            {METHODS.map((m) => (
              <button
                key={m.value}
                type="button"
                onClick={() => {
                  setMethod(m.value);
                  setError('');
                  if (m.value !== 'CASH') setReceived(total);
                }}
                className={`flex items-center gap-2 rounded-lg border px-3 py-3 text-sm font-medium transition-colors ${
                  method === m.value
                    ? 'border-blue-500 bg-blue-50 text-blue-700'
                    : 'border-gray-200 bg-white text-gray-600 hover:bg-gray-50'
                }`}
                aria-pressed={method === m.value}
              >
                <span aria-hidden="true">{m.icon}</span>
                {m.label}
              </button>
            ))}
          </div>
        </fieldset>

        {/* Monto recibido (solo efectivo) */}
        {isCash && (
          <div className="mb-4">
            <label htmlFor="payment-received" className="mb-1 block text-sm font-medium text-gray-700">
              Monto recibido
            </label>
            <div className="relative">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500">$</span>
              <input
                id="payment-received"
                type="number"
                min={total}
                step={100}
                value={received}
                onChange={(e) => {
                  setReceived(Number(e.target.value));
                  setError('');
                }}
                onFocus={(e) => e.target.select()}
                className="w-full rounded-lg border border-gray-300 py-3 pl-8 pr-4 text-right text-lg focus:border-blue-500 focus:outline-none"
              />
            </div>
            {change > 0 && (
              <p className="mt-1 text-sm text-green-600">
                Vuelto: <strong>${change.toLocaleString('es-CL')}</strong>
              </p>
            )}
            {received > 0 && received < total && (
              <p className="mt-1 text-xs text-amber-600">
                Faltan ${(total - received).toLocaleString('es-CL')}
              </p>
            )}
          </div>
        )}

        {/* Referencia (card/transfer) */}
        {!isCash && (
          <div className="mb-4">
            <label htmlFor="payment-reference" className="mb-1 block text-sm font-medium text-gray-700">
              Referencia <span className="text-gray-400">(opcional)</span>
            </label>
            <input
              id="payment-reference"
              type="text"
              value={reference}
              onChange={(e) => setReference(e.target.value)}
              placeholder="N° de autorización, voucher…"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
            />
          </div>
        )}

        {/* Error */}
        {error && (
          <p className="mb-3 text-sm text-red-600" role="alert">{error}</p>
        )}

        {/* Acciones */}
        <div className="flex gap-3">
          <button
            type="button"
            onClick={onCancel}
            className="flex-1 rounded-lg border border-gray-300 py-2.5 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            Cancelar
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            disabled={!canSubmit}
            className="flex-1 rounded-lg bg-blue-600 py-2.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
          >
            Confirmar pago
          </button>
        </div>
      </div>
    </div>
  );
}
