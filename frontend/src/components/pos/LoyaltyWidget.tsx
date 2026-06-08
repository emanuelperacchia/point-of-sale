import { useState, useEffect } from 'react';
import { loyaltyApi } from '../../services/api';
import type { PointsResponse } from '../../types';

interface Props {
  clientId: number | null;
  onRedeem?: (puntos: number) => void;
  disabled?: boolean;
}

const TIER_STYLES: Record<string, string> = {
  BRONCE: 'bg-amber-100 text-amber-800 border-amber-300',
  PLATA: 'bg-gray-100 text-gray-700 border-gray-300',
  ORO: 'bg-yellow-100 text-yellow-800 border-yellow-400',
};

export default function LoyaltyWidget({ clientId, onRedeem, disabled }: Props) {
  const [data, setData] = useState<PointsResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [redeemPuntos, setRedeemPuntos] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    if (!clientId) {
      return; // data already initialized as null
    }
    let cancelled = false;
    (async () => {
      setLoading(true);
      try {
        const { data } = await loyaltyApi.getPoints(clientId);
        if (!cancelled) setData(data);
      } catch {
        if (!cancelled) setData(null);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [clientId]);

  if (!clientId) return null;

  if (loading) {
    return (
      <div className="rounded-lg bg-white p-3 shadow-sm text-sm text-gray-400">
        Cargando puntos…
      </div>
    );
  }

  if (!data) return null;

  const handleRedeem = () => {
    const pts = Number(redeemPuntos);
    if (!pts || pts <= 0) { setError('Ingrese un valor válido'); return; }
    if (pts > data.saldoActual) { setError('Saldo insuficiente'); return; }
    setError('');
    onRedeem?.(pts);
    setRedeemPuntos('');
  };

  return (
    <div className="rounded-lg border bg-white p-3 shadow-sm">
      <div className="mb-2 flex items-center justify-between">
        <span className="text-xs font-semibold text-gray-600">Puntos</span>
        <span className={`rounded-full border px-2 py-0.5 text-xs font-bold ${TIER_STYLES[data.tier] ?? TIER_STYLES.BRONCE}`}>
          {data.tier}
        </span>
      </div>
      <p className="text-lg font-bold text-gray-800">
        {data.saldoActual.toLocaleString('es-CL')} pts
      </p>
      {onRedeem && data.saldoActual > 0 && (
        <div className="mt-2 flex gap-1">
          <input
            type="number"
            min={1}
            max={data.saldoActual}
            value={redeemPuntos}
            onChange={(e) => { setRedeemPuntos(e.target.value); setError(''); }}
            placeholder="Puntos a canjear"
            disabled={disabled}
            className="w-full rounded border border-gray-300 px-2 py-1 text-xs focus:border-blue-500 focus:outline-none disabled:opacity-50"
          />
          <button
            onClick={handleRedeem}
            disabled={disabled || !redeemPuntos}
            className="rounded bg-purple-600 px-3 py-1 text-xs font-medium text-white hover:bg-purple-700 disabled:opacity-50"
          >
            Canjear
          </button>
        </div>
      )}
      {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
    </div>
  );
}
