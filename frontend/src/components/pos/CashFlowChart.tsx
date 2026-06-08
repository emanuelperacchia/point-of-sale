import { useState, useEffect } from 'react';
import {
  Bar, Line, XAxis, YAxis, CartesianGrid,
  Tooltip, Legend, ResponsiveContainer, ComposedChart,
} from 'recharts';
import { reportApi } from '../../services/api';
import type { CashFlowDayRow, CashFlowResponse } from '../../types';

interface WeekGroup {
  week: string;
  ingresos: number;
  egresos: number;
  saldoAcumulado: number;
}

function groupByWeek(days: CashFlowDayRow[]): WeekGroup[] {
  const groups: Record<string, WeekGroup> = {};
  for (const day of days) {
    if (day.esProyectado) continue; // Only real data for chart
    const date = new Date(day.fecha);
    const weekStart = new Date(date);
    weekStart.setDate(date.getDate() - date.getDay());
    const key = weekStart.toISOString().slice(0, 10);

    if (!groups[key]) {
      groups[key] = { week: key, ingresos: 0, egresos: 0, saldoAcumulado: 0 };
    }
    groups[key].ingresos += day.ingresos;
    groups[key].egresos += day.egresos;
    groups[key].saldoAcumulado = day.saldoAcumulado;
  }
  return Object.values(groups).sort((a, b) => a.week.localeCompare(b.week));
}

export default function CashFlowChart() {
  const [data, setData] = useState<CashFlowResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const today = new Date();
    const monthAgo = new Date(today);
    monthAgo.setDate(today.getDate() - 30);

    const desde = monthAgo.toISOString().slice(0, 10);
    const hasta = today.toISOString().slice(0, 10);

    reportApi
      .getCashFlow({ desde, hasta, incluirProyeccion: true, diasProyeccion: 30 })
      .then((res) => setData(res.data))
      .catch(() => setError('Error al cargar flujo de caja'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="rounded-lg bg-white p-4 shadow-sm">
        <p className="text-sm text-gray-400">Cargando flujo de caja…</p>
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="rounded-lg bg-white p-4 shadow-sm">
        <p className="text-sm text-red-500">{error || 'Sin datos'}</p>
      </div>
    );
  }

  const weeks = groupByWeek(data.dias);

  return (
    <section className="rounded-lg bg-white p-4 shadow-sm" aria-label="Flujo de caja">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="font-semibold text-gray-700">Flujo de Caja</h2>
        {data.alertaSaldoNegativo && (
          <span className="rounded bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700" role="alert">
            ⚠ Saldo negativo proyectado: {data.primerDiaSaldoNegativo}
          </span>
        )}
      </div>

      {weeks.length === 0 ? (
        <p className="text-sm text-gray-400">No hay datos suficientes para mostrar el gráfico.</p>
      ) : (
        <ResponsiveContainer width="100%" height={200}>
          <ComposedChart data={weeks}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="week" tick={{ fontSize: 10 }} tickFormatter={(v: string) => v.slice(5)} />
            <YAxis tick={{ fontSize: 10 }} />
            <Tooltip />
            <Legend />
            <Bar dataKey="ingresos" fill="#22c55e" name="Ingresos" radius={[2, 2, 0, 0]} />
            <Bar dataKey="egresos" fill="#ef4444" name="Egresos" radius={[2, 2, 0, 0]} />
            <Line type="monotone" dataKey="saldoAcumulado" stroke="#3b82f6" strokeWidth={2} name="Saldo Acum." dot={false} />
          </ComposedChart>
        </ResponsiveContainer>
      )}

      {data.alertaSaldoNegativo && (
        <p className="mt-2 text-xs text-red-600">
          La proyección muestra saldo negativo a partir del {data.primerDiaSaldoNegativo}.
          Revise los gastos proyectados.
        </p>
      )}
    </section>
  );
}
