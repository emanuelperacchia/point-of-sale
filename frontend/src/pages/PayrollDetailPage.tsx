import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { payrollApi } from '../services/api';
import type { PayrollResponse, PayrollAdjustmentResponse } from '../types';
import Layout from '../components/common/Layout';

function DetailRow({ label, value, highlight }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div className={`flex justify-between border-b py-2 text-sm ${highlight ? 'font-semibold' : ''}`}>
      <span className="text-gray-600">{label}</span>
      <span>{value}</span>
    </div>
  );
}

export default function PayrollDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [payroll, setPayroll] = useState<PayrollResponse | null>(null);
  const [adjustments, setAdjustments] = useState<PayrollAdjustmentResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Adjustment form
  const [concepto, setConcepto] = useState('');
  const [monto, setMonto] = useState('');
  const [justificacion, setJustificacion] = useState('');
  const [showAdjustForm, setShowAdjustForm] = useState(false);

  useEffect(() => {
    if (id) {
      loadPayroll(Number(id));
      loadAdjustments(Number(id));
    }
  }, [id]);

  function loadPayroll(payrollId: number) {
    setLoading(true);
    payrollApi
      .getById(payrollId)
      .then((res) => setPayroll(res.data))
      .catch(() => setError('Error al cargar liquidacion'))
      .finally(() => setLoading(false));
  }

  function loadAdjustments(payrollId: number) {
    payrollApi
      .listAdjustments(payrollId)
      .then((res) => setAdjustments(res.data))
      .catch(() => {});
  }

  function handleApprove() {
    if (!payroll) return;
    const aprobadoPor = 1; // TODO: from auth context
    payrollApi
      .approve(payroll.id, aprobadoPor)
      .then(() => loadPayroll(payroll.id))
      .catch((err) => setError(err.response?.data?.message || 'Error al aprobar'));
  }

  function handleDownloadPdf() {
    if (!payroll) return;
    payrollApi
      .generatePdf(payroll.id)
      .then((res) => {
        const url = URL.createObjectURL(new Blob([res.data]));
        const a = document.createElement('a');
        a.href = url;
        a.download = `recibo-sueldo-${payroll.id}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
      })
      .catch(() => setError('Error al generar PDF'));
  }

  function handleAddAdjustment(e: React.FormEvent) {
    e.preventDefault();
    if (!payroll) return;

    const creadoPor = 1; // TODO: from auth context
    payrollApi
      .addAdjustment(payroll.id, concepto, Number(monto), creadoPor, justificacion || undefined)
      .then(() => {
        setConcepto('');
        setMonto('');
        setJustificacion('');
        setShowAdjustForm(false);
        loadAdjustments(payroll.id);
      })
      .catch((err) => setError(err.response?.data?.message || 'Error al agregar ajuste'));
  }

  if (loading) {
    return (
      <Layout title="Cargando...">
        <div className="flex items-center justify-center py-12">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-600 border-t-transparent" />
        </div>
      </Layout>
    );
  }

  if (!payroll) {
    return (
      <Layout title="Error">
        <div className="text-center py-12 text-gray-500">{error || 'Liquidacion no encontrada'}</div>
      </Layout>
    );
  }

  return (
    <Layout title="Detalle de Liquidacion">
      <div className="space-y-4">
        {error && (
          <div className="rounded border border-red-300 bg-red-50 px-4 py-2 text-sm text-red-700">{error}</div>
        )}

        {/* ── Actions ─────────────────────────────────────────────── */}
        <div className="flex gap-2 rounded-lg bg-white p-4 shadow">
          <button
            onClick={() => navigate('/payroll')}
            className="rounded bg-gray-600 px-3 py-1.5 text-sm text-white hover:bg-gray-700"
          >
            Volver
          </button>
          {payroll.estado === 'BORRADOR' && (
            <button
              onClick={handleApprove}
              className="rounded bg-green-600 px-3 py-1.5 text-sm text-white hover:bg-green-700"
            >
              Aprobar
            </button>
          )}
          <button
            onClick={handleDownloadPdf}
            className="rounded bg-blue-600 px-3 py-1.5 text-sm text-white hover:bg-blue-700"
          >
            Descargar PDF
          </button>
          <button
            onClick={() => setShowAdjustForm(!showAdjustForm)}
            className="rounded bg-orange-600 px-3 py-1.5 text-sm text-white hover:bg-orange-700"
          >
            {showAdjustForm ? 'Cancelar Ajuste' : 'Agregar Ajuste'}
          </button>
        </div>

        {/* ── Adjustment Form ──────────────────────────────────────── */}
        {showAdjustForm && (
          <form onSubmit={handleAddAdjustment} className="rounded-lg bg-white p-4 shadow">
            <h4 className="mb-3 text-sm font-semibold text-gray-800">Nuevo Ajuste</h4>
            <div className="flex flex-wrap gap-3">
              <div>
                <label className="block text-xs text-gray-600">Concepto</label>
                <input
                  value={concepto}
                  onChange={(e) => setConcepto(e.target.value)}
                  required
                  className="mt-1 rounded border px-3 py-1.5 text-sm"
                />
              </div>
              <div>
                <label className="block text-xs text-gray-600">Monto</label>
                <input
                  type="number"
                  step="0.01"
                  value={monto}
                  onChange={(e) => setMonto(e.target.value)}
                  required
                  className="mt-1 rounded border px-3 py-1.5 text-sm"
                />
              </div>
              <div>
                <label className="block text-xs text-gray-600">Justificacion</label>
                <input
                  value={justificacion}
                  onChange={(e) => setJustificacion(e.target.value)}
                  className="mt-1 rounded border px-3 py-1.5 text-sm"
                />
              </div>
              <button
                type="submit"
                className="self-end rounded bg-orange-600 px-4 py-1.5 text-sm text-white hover:bg-orange-700"
              >
                Guardar
              </button>
            </div>
          </form>
        )}

        {/* ── Payroll Detail ───────────────────────────────────────── */}
        <div className="grid gap-4 md:grid-cols-2">
          {/* Haberes */}
          <div className="rounded-lg bg-white p-4 shadow">
            <h4 className="mb-2 text-sm font-semibold text-gray-800">Haberes</h4>
            <DetailRow label="Periodo" value={`${payroll.mes}/${payroll.anio}`} />
            <DetailRow label="Dias Trabajados" value={`${payroll.diasTrabajados}`} />
            <DetailRow label="Horas Normales" value={`${payroll.horasNormalesMinutos} min`} />
            <DetailRow label="Horas Extra" value={`${payroll.horasExtraMinutos} min`} />
            <DetailRow label="Sueldo Basico" value={`$${payroll.sueldoBasico?.toFixed(2)}`} />
            <DetailRow label="Plus Horas Extra" value={`$${payroll.plusHorasExtra?.toFixed(2)}`} />
            <DetailRow label="Comisiones" value={`$${payroll.comisiones?.toFixed(2)}`} />
            <DetailRow label="Bono Desempeno" value={`$${payroll.bonoDesempeno?.toFixed(2)}`} />
            <DetailRow label="Total Haberes" value={`$${payroll.totalHaberes?.toFixed(2)}`} highlight />
          </div>

          {/* Descuentos */}
          <div className="rounded-lg bg-white p-4 shadow">
            <h4 className="mb-2 text-sm font-semibold text-gray-800">Descuentos</h4>
            <DetailRow label="Jubilacion" value={`-$${payroll.descJubilacion?.toFixed(2)}`} />
            <DetailRow label="Obra Social" value={`-$${payroll.descObraSocial?.toFixed(2)}`} />
            <DetailRow label="ANSES" value={`-$${payroll.descAnses?.toFixed(2)}`} />
            <DetailRow label="Ausencias" value={`-$${payroll.descAusencias?.toFixed(2)}`} />
            <DetailRow label="Embargos" value={`-$${payroll.descEmbargos?.toFixed(2)}`} />
            <DetailRow label="Total Descuentos" value={`-$${payroll.totalDescuentos?.toFixed(2)}`} highlight />
            <div className="mt-3 border-t pt-3">
              <DetailRow
                label="NETO A PAGAR"
                value={`$${payroll.netoApagar?.toFixed(2)}`}
                highlight
              />
            </div>
            <div className="mt-2">
              <span
                className={`rounded px-2 py-0.5 text-xs font-medium ${
                  payroll.estado === 'APROBADA'
                    ? 'bg-green-100 text-green-700'
                    : 'bg-yellow-100 text-yellow-700'
                }`}
              >
                {payroll.estado}
              </span>
              {payroll.fechaAprobacion && (
                <span className="ml-2 text-xs text-gray-500">
                  Aprobado: {payroll.fechaAprobacion}
                </span>
              )}
            </div>
          </div>
        </div>

        {/* ── Adjustments ──────────────────────────────────────────── */}
        {adjustments.length > 0 && (
          <div className="rounded-lg bg-white p-4 shadow">
            <h4 className="mb-2 text-sm font-semibold text-gray-800">Ajustes</h4>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b bg-gray-50">
                    <th className="px-3 py-2 font-medium text-gray-600">Concepto</th>
                    <th className="px-3 py-2 font-medium text-gray-600">Monto</th>
                    <th className="px-3 py-2 font-medium text-gray-600">Justificacion</th>
                  </tr>
                </thead>
                <tbody>
                  {adjustments.map((a) => (
                    <tr key={a.id} className="border-b">
                      <td className="px-3 py-2">{a.concepto}</td>
                      <td className="px-3 py-2">${a.monto?.toFixed(2)}</td>
                      <td className="px-3 py-2 text-gray-500">{a.justificacion || '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
}
