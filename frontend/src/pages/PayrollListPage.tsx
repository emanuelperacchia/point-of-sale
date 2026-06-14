import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { payrollApi, employeeApi } from '../services/api';
import type { PayrollResponse, EmployeeResponse } from '../types';
import Layout from '../components/common/Layout';

export default function PayrollListPage() {
  const navigate = useNavigate();
  const [payrolls, setPayrolls] = useState<PayrollResponse[]>([]);
  const [employees, setEmployees] = useState<EmployeeResponse[]>([]);

  const now = new Date();
  const [mes, setMes] = useState(now.getMonth() + 1);
  const [anio, setAnio] = useState(now.getFullYear());

  const [error, setError] = useState('');

  useEffect(() => {
    employeeApi.getAll().then((res) => setEmployees(res.data)).catch(() => {});
  }, []);

  useEffect(() => {
    payrollApi
      .list({ mes, anio })
      .then((res) => setPayrolls(res.data))
      .catch(() => setPayrolls([]));
  }, [mes, anio]);

  function employeeName(employeeId: number): string {
    const emp = employees.find((e) => e.id === employeeId);
    return emp ? `${emp.apellido}, ${emp.nombre}` : `Empleado #${employeeId}`;
  }

  function handleApprove(id: number) {
    const aprobadoPor = 1; // TODO: get from auth context
    payrollApi
      .approve(id, aprobadoPor)
      .then(() => {
        payrollApi.list({ mes, anio }).then((res) => setPayrolls(res.data)).catch(() => {});
      })
      .catch((err) => setError(err.response?.data?.message || 'Error al aprobar'));
  }

  function handleExportCsv() {
    payrollApi
      .exportCsv(mes, anio)
      .then((res) => {
        const url = URL.createObjectURL(new Blob([res.data]));
        const a = document.createElement('a');
        a.href = url;
        a.download = `sueldos-${mes}-${anio}.csv`;
        a.click();
        URL.revokeObjectURL(url);
      })
      .catch(() => setError('Error al exportar CSV'));
  }

  return (
    <Layout title="Liquidacion de Sueldos">
      <div className="space-y-4">
        {error && (
          <div className="rounded border border-red-300 bg-red-50 px-4 py-2 text-sm text-red-700">{error}</div>
        )}

        {/* ── Filtros ─────────────────────────────────────────────── */}
        <div className="flex items-end gap-3 rounded-lg bg-white p-4 shadow">
          <div>
            <label className="block text-xs text-gray-600">Mes</label>
            <input
              type="number"
              min={1}
              max={12}
              value={mes}
              onChange={(e) => setMes(Number(e.target.value))}
              className="mt-1 w-16 rounded border px-3 py-1.5 text-sm"
            />
          </div>
          <div>
            <label className="block text-xs text-gray-600">Anio</label>
            <input
              type="number"
              value={anio}
              onChange={(e) => setAnio(Number(e.target.value))}
              className="mt-1 w-20 rounded border px-3 py-1.5 text-sm"
            />
          </div>
          <button
            onClick={handleExportCsv}
            className="rounded bg-gray-600 px-4 py-1.5 text-sm text-white hover:bg-gray-700"
          >
            Exportar CSV
          </button>
        </div>

        {/* ── Tabla ────────────────────────────────────────────────── */}
        <div className="overflow-x-auto rounded-lg bg-white shadow">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b bg-gray-50">
                <th className="px-3 py-2 font-medium text-gray-600">Empleado</th>
                <th className="px-3 py-2 font-medium text-gray-600">Dias</th>
                <th className="px-3 py-2 font-medium text-gray-600">Haberes</th>
                <th className="px-3 py-2 font-medium text-gray-600">Descuentos</th>
                <th className="px-3 py-2 font-medium text-gray-600">Neto</th>
                <th className="px-3 py-2 font-medium text-gray-600">Estado</th>
                <th className="px-3 py-2 font-medium text-gray-600">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {payrolls.map((p) => (
                <tr key={p.id} className="border-b hover:bg-gray-50">
                  <td className="px-3 py-2">{employeeName(p.employeeId)}</td>
                  <td className="px-3 py-2">{p.diasTrabajados}</td>
                  <td className="px-3 py-2">${p.totalHaberes?.toFixed(2)}</td>
                  <td className="px-3 py-2">${p.totalDescuentos?.toFixed(2)}</td>
                  <td className="px-3 py-2 font-medium">${p.netoApagar?.toFixed(2)}</td>
                  <td className="px-3 py-2">
                    <span
                      className={`rounded px-2 py-0.5 text-xs font-medium ${
                        p.estado === 'APROBADA'
                          ? 'bg-green-100 text-green-700'
                          : 'bg-yellow-100 text-yellow-700'
                      }`}
                    >
                      {p.estado}
                    </span>
                  </td>
                  <td className="px-3 py-2">
                    <div className="flex gap-2">
                      <button
                        onClick={() => navigate(`/payroll/${p.id}`)}
                        className="rounded bg-blue-600 px-2 py-1 text-xs text-white hover:bg-blue-700"
                      >
                        Ver
                      </button>
                      {p.estado === 'BORRADOR' && (
                        <button
                          onClick={() => handleApprove(p.id)}
                          className="rounded bg-green-600 px-2 py-1 text-xs text-white hover:bg-green-700"
                        >
                          Aprobar
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
              {payrolls.length === 0 && (
                <tr>
                  <td colSpan={7} className="px-3 py-8 text-center text-gray-400">
                    No hay liquidaciones para este periodo
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* ── Totales ──────────────────────────────────────────────── */}
        {payrolls.length > 0 && (
          <div className="rounded-lg bg-white p-4 shadow">
            <p className="text-sm text-gray-600">
              Total liquidado:{' '}
              <span className="font-semibold text-gray-800">
                ${payrolls.reduce((sum, p) => sum + (p.netoApagar || 0), 0).toFixed(2)}
              </span>
            </p>
          </div>
        )}
      </div>
    </Layout>
  );
}
