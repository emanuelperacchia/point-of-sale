import { useState, useEffect } from 'react';
import { commissionApi, employeeApi } from '../services/api';
import type {
  CommissionSchemeResponse,
  CommissionSchemeRequest,
  CommissionResultResponse,
  EmployeeResponse,
} from '../types';
import Layout from '../components/common/Layout';

export default function CommissionPage() {
  const [schemes, setSchemes] = useState<CommissionSchemeResponse[]>([]);
  const [ranking, setRanking] = useState<CommissionResultResponse[]>([]);
  const [employees, setEmployees] = useState<EmployeeResponse[]>([]);

  // Form state
  const [nombre, setNombre] = useState('');
  const [tipo, setTipo] = useState<'PORCENTAJE_VENTA' | 'ESCALONADO'>('PORCENTAJE_VENTA');
  const [valor, setValor] = useState('');

  // Calculate state
  const [calcEmployeeId, setCalcEmployeeId] = useState('');
  const [calcMes, setCalcMes] = useState(new Date().getMonth() + 1);
  const [calcAnio, setCalcAnio] = useState(new Date().getFullYear());
  const [calcResult, setCalcResult] = useState<CommissionResultResponse | null>(null);

  // Ranking period
  const [rankMes, setRankMes] = useState(new Date().getMonth() + 1);
  const [rankAnio, setRankAnio] = useState(new Date().getFullYear());

  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    loadSchemes();
    loadEmployees();
  }, []);

  function loadSchemes() {
    commissionApi.listSchemes().then((res) => setSchemes(res.data)).catch(() => {});
  }

  function loadEmployees() {
    employeeApi.getAll().then((res) => setEmployees(res.data)).catch(() => {});
  }

  function loadRanking() {
    commissionApi.ranking(rankMes, rankAnio).then((res) => setRanking(res.data)).catch(() => {});
  }

  function handleCreateScheme(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setSuccess('');

    const payload: CommissionSchemeRequest = {
      nombre,
      tipo,
      ...(tipo === 'PORCENTAJE_VENTA' && valor ? { valor: Number(valor) } : {}),
    };

    commissionApi
      .createScheme(payload)
      .then(() => {
        setSuccess('Esquema creado exitosamente');
        setNombre('');
        setValor('');
        loadSchemes();
      })
      .catch((err) => setError(err.response?.data?.message || 'Error al crear esquema'));
  }

  function handleCalculate(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setCalcResult(null);

    commissionApi
      .calculate(Number(calcEmployeeId), calcMes, calcAnio)
      .then((res) => setCalcResult(res.data))
      .catch((err) => setError(err.response?.data?.message || 'Error al calcular comisiones'));
  }

  return (
    <Layout title="Comisiones">
      <div className="space-y-6">
        {error && (
          <div className="rounded border border-red-300 bg-red-50 px-4 py-2 text-sm text-red-700">{error}</div>
        )}
        {success && (
          <div className="rounded border border-green-300 bg-green-50 px-4 py-2 text-sm text-green-700">
            {success}
          </div>
        )}

        {/* ── Crear esquema ───────────────────────────────────────── */}
        <section className="rounded-lg bg-white p-4 shadow">
          <h3 className="mb-3 text-base font-semibold text-gray-800">Crear Esquema de Comision</h3>
          <form onSubmit={handleCreateScheme} className="flex flex-wrap items-end gap-3">
            <div>
              <label className="block text-xs text-gray-600">Nombre</label>
              <input
                value={nombre}
                onChange={(e) => setNombre(e.target.value)}
                required
                className="mt-1 rounded border px-3 py-1.5 text-sm"
              />
            </div>
            <div>
              <label className="block text-xs text-gray-600">Tipo</label>
              <select
                value={tipo}
                onChange={(e) => setTipo(e.target.value as 'PORCENTAJE_VENTA' | 'ESCALONADO')}
                className="mt-1 rounded border px-3 py-1.5 text-sm"
              >
                <option value="PORCENTAJE_VENTA">Porcentaje de Venta</option>
                <option value="ESCALONADO">Escalonado</option>
              </select>
            </div>
            <div>
              <label className="block text-xs text-gray-600">Valor (%)</label>
              <input
                type="number"
                step="0.01"
                value={valor}
                onChange={(e) => setValor(e.target.value)}
                className="mt-1 rounded border px-3 py-1.5 text-sm"
              />
            </div>
            <button
              type="submit"
              className="rounded bg-blue-600 px-4 py-1.5 text-sm text-white hover:bg-blue-700"
            >
              Crear
            </button>
          </form>
        </section>

        {/* ── Esquemas existentes ─────────────────────────────────── */}
        <section className="rounded-lg bg-white p-4 shadow">
          <h3 className="mb-3 text-base font-semibold text-gray-800">Esquemas Existentes</h3>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b bg-gray-50">
                  <th className="px-3 py-2 font-medium text-gray-600">Nombre</th>
                  <th className="px-3 py-2 font-medium text-gray-600">Tipo</th>
                  <th className="px-3 py-2 font-medium text-gray-600">Valor</th>
                  <th className="px-3 py-2 font-medium text-gray-600">Activo</th>
                </tr>
              </thead>
              <tbody>
                {schemes.map((s) => (
                  <tr key={s.id} className="border-b hover:bg-gray-50">
                    <td className="px-3 py-2">{s.nombre}</td>
                    <td className="px-3 py-2">{s.tipo}</td>
                    <td className="px-3 py-2">{s.valor}%</td>
                    <td className="px-3 py-2">{s.activo ? 'Si' : 'No'}</td>
                  </tr>
                ))}
                {schemes.length === 0 && (
                  <tr>
                    <td colSpan={4} className="px-3 py-4 text-center text-gray-400">
                      No hay esquemas registrados
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>

        {/* ── Calcular comisiones ─────────────────────────────────── */}
        <section className="rounded-lg bg-white p-4 shadow">
          <h3 className="mb-3 text-base font-semibold text-gray-800">Calcular Comisiones</h3>
          <form onSubmit={handleCalculate} className="flex flex-wrap items-end gap-3">
            <div>
              <label className="block text-xs text-gray-600">Empleado</label>
              <select
                value={calcEmployeeId}
                onChange={(e) => setCalcEmployeeId(e.target.value)}
                required
                className="mt-1 rounded border px-3 py-1.5 text-sm"
              >
                <option value="">Seleccionar...</option>
                {employees.map((emp) => (
                  <option key={emp.id} value={emp.id}>
                    {emp.apellido}, {emp.nombre}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs text-gray-600">Mes</label>
              <input
                type="number"
                min={1}
                max={12}
                value={calcMes}
                onChange={(e) => setCalcMes(Number(e.target.value))}
                className="mt-1 w-16 rounded border px-3 py-1.5 text-sm"
              />
            </div>
            <div>
              <label className="block text-xs text-gray-600">Anio</label>
              <input
                type="number"
                value={calcAnio}
                onChange={(e) => setCalcAnio(Number(e.target.value))}
                className="mt-1 w-20 rounded border px-3 py-1.5 text-sm"
              />
            </div>
            <button
              type="submit"
              className="rounded bg-green-600 px-4 py-1.5 text-sm text-white hover:bg-green-700"
            >
              Calcular
            </button>
          </form>

          {calcResult && (
            <div className="mt-4 rounded border bg-gray-50 p-3 text-sm">
              <p>
                <strong>Total Ventas:</strong> ${calcResult.totalVentas?.toFixed(2)}
              </p>
              <p>
                <strong>Comision Calculada:</strong> ${calcResult.comisionCalculada?.toFixed(2)}
              </p>
              <p>
                <strong>Meta Alcanzada:</strong> {calcResult.metaAlcanzada ? 'Si' : 'No'}
              </p>
              {calcResult.bonoAplicado > 0 && (
                <p>
                  <strong>Bono Aplicado:</strong> ${calcResult.bonoAplicado?.toFixed(2)}
                </p>
              )}
              <p>
                <strong>Esquema:</strong> {calcResult.esquemaUsado}
              </p>
            </div>
          )}
        </section>

        {/* ── Ranking ─────────────────────────────────────────────── */}
        <section className="rounded-lg bg-white p-4 shadow">
          <h3 className="mb-3 text-base font-semibold text-gray-800">Ranking de Vendedores</h3>
          <div className="mb-3 flex items-end gap-3">
            <div>
              <label className="block text-xs text-gray-600">Mes</label>
              <input
                type="number"
                min={1}
                max={12}
                value={rankMes}
                onChange={(e) => setRankMes(Number(e.target.value))}
                className="mt-1 w-16 rounded border px-3 py-1.5 text-sm"
              />
            </div>
            <div>
              <label className="block text-xs text-gray-600">Anio</label>
              <input
                type="number"
                value={rankAnio}
                onChange={(e) => setRankAnio(Number(e.target.value))}
                className="mt-1 w-20 rounded border px-3 py-1.5 text-sm"
              />
            </div>
            <button
              onClick={loadRanking}
              className="rounded bg-purple-600 px-4 py-1.5 text-sm text-white hover:bg-purple-700"
            >
              Ver Ranking
            </button>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b bg-gray-50">
                  <th className="px-3 py-2 font-medium text-gray-600">#</th>
                  <th className="px-3 py-2 font-medium text-gray-600">Empleado</th>
                  <th className="px-3 py-2 font-medium text-gray-600">Total Ventas</th>
                  <th className="px-3 py-2 font-medium text-gray-600">Comision</th>
                  <th className="px-3 py-2 font-medium text-gray-600">Bono</th>
                </tr>
              </thead>
              <tbody>
                {ranking.map((r, i) => (
                  <tr key={r.id} className="border-b hover:bg-gray-50">
                    <td className="px-3 py-2">{i + 1}</td>
                    <td className="px-3 py-2">Empleado #{r.employeeId}</td>
                    <td className="px-3 py-2">${r.totalVentas?.toFixed(2)}</td>
                    <td className="px-3 py-2">${r.comisionCalculada?.toFixed(2)}</td>
                    <td className="px-3 py-2">${r.bonoAplicado?.toFixed(2)}</td>
                  </tr>
                ))}
                {ranking.length === 0 && (
                  <tr>
                    <td colSpan={5} className="px-3 py-4 text-center text-gray-400">
                      Sin datos para el periodo seleccionado
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </Layout>
  );
}
