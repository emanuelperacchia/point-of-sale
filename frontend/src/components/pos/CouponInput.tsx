import { useState } from 'react';
import { couponApi } from '../../services/api';
import type { CouponValidationResponse } from '../../types';

interface Props {
  onCouponApplied: (coupon: CouponValidationResponse) => void;
  disabled?: boolean;
}

export default function CouponInput({ onCouponApplied, disabled }: Props) {
  const [code, setCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<CouponValidationResponse | null>(null);

  const handleValidate = async () => {
    if (!code.trim()) return;
    setLoading(true);
    setResult(null);
    try {
      const { data } = await couponApi.validate(code.trim());
      setResult(data);
      if (data.valido) {
        onCouponApplied(data);
      }
    } catch {
      setResult({ id: null, codigo: code, tipo: null, valor: 0, valido: false, mensaje: 'Error al validar cupón' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-2">
      <label className="block text-sm font-medium text-gray-700">
        Cupón de descuento
      </label>
      <div className="flex gap-2">
        <input
          type="text"
          value={code}
          onChange={(e) => { setCode(e.target.value.toUpperCase()); setResult(null); }}
          onKeyDown={(e) => { if (e.key === 'Enter') handleValidate(); }}
          placeholder="Ingrese código"
          disabled={disabled}
          className="flex-1 rounded-lg border border-gray-300 px-3 py-2 text-sm uppercase focus:border-blue-500 focus:outline-none disabled:opacity-50"
        />
        <button
          onClick={handleValidate}
          disabled={disabled || loading || !code.trim()}
          className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {loading ? '…' : 'Aplicar'}
        </button>
      </div>
      {result && (
        <p className={`text-sm ${result.valido ? 'text-green-600' : 'text-red-600'}`} role="alert">
          {result.valido
            ? `✅ Cupón aplicado: ${result.tipo === 'PORCENTAJE' ? `${result.valor}% OFF` : `$${result.valor.toLocaleString('es-CL')}`}`
            : `❌ ${result.mensaje}`}
        </p>
      )}
    </div>
  );
}
