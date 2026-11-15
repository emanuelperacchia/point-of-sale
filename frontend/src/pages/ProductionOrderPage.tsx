import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { productionOrderApi, recipeApi, employeeApi } from '../services/api';
import type { ProductionOrderResponse, RecipeResponse, EmployeeResponse, ProductionOrderRequest } from '../types';
import Layout from '../components/common/Layout';

export default function ProductionOrderPage() {
  const navigate = useNavigate();
  const [orders, setOrders] = useState<ProductionOrderResponse[]>([]);
  const [recipes, setRecipes] = useState<RecipeResponse[]>([]);
  const [employees, setEmployees] = useState<EmployeeResponse[]>([]);
  const [estadoFilter, setEstadoFilter] = useState('');

  // Create form
  const [showForm, setShowForm] = useState(false);
  const [recipeId, setRecipeId] = useState('');
  const [cantidad, setCantidad] = useState('');
  const [fecha, setFecha] = useState(new Date().toISOString().split('T')[0]);
  const [responsableId, setResponsableId] = useState('');
  const [observaciones, setObservaciones] = useState('');

  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    loadOrders();
    recipeApi.list().then((res) => setRecipes(res.data)).catch(() => {});
    employeeApi.getAll().then((res) => setEmployees(res.data)).catch(() => {});
  }, []);

  function loadOrders() {
    productionOrderApi
      .list(estadoFilter || undefined)
      .then((res) => setOrders(res.data))
      .catch(() => setOrders([]));
  }

  function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setSuccess('');

    const payload: ProductionOrderRequest = {
      recipeId: Number(recipeId),
      cantidadPlanificada: Number(cantidad),
      fechaPlanificada: fecha,
      responsableId: Number(responsableId),
      observaciones: observaciones || undefined,
    };

    productionOrderApi
      .create(payload)
      .then(() => {
        setSuccess('Orden de produccion creada');
        setShowForm(false);
        setRecipeId('');
        setCantidad('');
        setResponsableId('');
        setObservaciones('');
        loadOrders();
      })
      .catch((err) => setError(err.response?.data?.message || 'Error al crear'));
  }

  function handleStart(id: number) {
    productionOrderApi
      .start(id)
      .then(() => loadOrders())
      .catch((err) => setError(err.response?.data?.message || 'Error al iniciar'));
  }

  function handleCancel(id: number) {
    if (!confirm('Cancelar orden de produccion?')) return;
    productionOrderApi
      .cancel(id)
      .then(() => loadOrders())
      .catch((err) => setError(err.response?.data?.message || 'Error al cancelar'));
  }

  function estadoBadge(estado: string) {
    const colors: Record<string, string> = {
      PLANIFICADA: 'bg-blue-100 text-blue-700',
      EN_PROCESO: 'bg-yellow-100 text-yellow-700',
      COMPLETADA: 'bg-green-100 text-green-700',
      CANCELADA: 'bg-red-100 text-red-700',
    };
    return <span className={`rounded px-2 py-0.5 text-xs font-medium ${colors[estado] || 'bg-surface-raised/30 text-ink'}`}>{estado}</span>;
  }

  return (
    <Layout title="Ordenes de Produccion">
      <div className="space-y-4">
        {error && <div className="rounded border border-red-300 bg-red-50 px-4 py-2 text-sm text-red-700">{error}</div>}
        {success && <div className="rounded border border-green-300 bg-green-50 px-4 py-2 text-sm text-green-700">{success}</div>}

        {/* ── Actions bar ─────────────────────────────────────────── */}
        <div className="flex items-center gap-3 rounded-lg bg-surface p-4 shadow">
          <div>
            <label className="block text-xs text-ink-soft">Filtrar estado</label>
            <select value={estadoFilter} onChange={(e) => { setEstadoFilter(e.target.value); }} className="mt-1 rounded border px-3 py-1.5 text-sm">
              <option value="">Todos</option>
              <option value="PLANIFICADA">Planificadas</option>
              <option value="EN_PROCESO">En Proceso</option>
              <option value="COMPLETADA">Completadas</option>
              <option value="CANCELADA">Canceladas</option>
            </select>
          </div>
          <button onClick={loadOrders} className="rounded bg-gray-600 px-4 py-1.5 text-sm text-white hover:bg-gray-700">Filtrar</button>
          <button onClick={() => setShowForm(!showForm)} className="rounded bg-blue-600 px-4 py-1.5 text-sm text-white hover:bg-blue-700">
            {showForm ? 'Cancelar' : 'Nueva Orden'}
          </button>
        </div>

        {/* ── Create form ─────────────────────────────────────────── */}
        {showForm && (
          <form onSubmit={handleCreate} className="rounded-lg bg-surface p-4 shadow">
            <h4 className="mb-3 text-sm font-semibold text-ink">Nueva Orden de Produccion</h4>
            <div className="flex flex-wrap gap-3">
              <div>
                <label className="block text-xs text-ink-soft">Receta</label>
                <select value={recipeId} onChange={(e) => setRecipeId(e.target.value)} required className="mt-1 rounded border px-3 py-1.5 text-sm">
                  <option value="">Seleccionar...</option>
                  {recipes.filter((r) => r.activa).map((r) => (
                    <option key={r.id} value={r.id}>{r.nombre}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs text-ink-soft">Cantidad</label>
                <input type="number" min={1} value={cantidad} onChange={(e) => setCantidad(e.target.value)} required className="mt-1 w-20 rounded border px-3 py-1.5 text-sm" />
              </div>
              <div>
                <label className="block text-xs text-ink-soft">Fecha</label>
                <input type="date" value={fecha} onChange={(e) => setFecha(e.target.value)} required className="mt-1 rounded border px-3 py-1.5 text-sm" />
              </div>
              <div>
                <label className="block text-xs text-ink-soft">Responsable</label>
                <select value={responsableId} onChange={(e) => setResponsableId(e.target.value)} required className="mt-1 rounded border px-3 py-1.5 text-sm">
                  <option value="">Seleccionar...</option>
                  {employees.map((emp) => (
                    <option key={emp.id} value={emp.id}>{emp.apellido}, {emp.nombre}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs text-ink-soft">Observaciones</label>
                <input value={observaciones} onChange={(e) => setObservaciones(e.target.value)} className="mt-1 rounded border px-3 py-1.5 text-sm" />
              </div>
            </div>
            <button type="submit" className="mt-3 rounded bg-blue-600 px-4 py-1.5 text-sm text-white hover:bg-blue-700">Crear</button>
          </form>
        )}

        {/* ── Table ────────────────────────────────────────────────── */}
        <div className="overflow-x-auto rounded-lg bg-surface shadow">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b bg-surface/50">
                <th className="px-3 py-2 font-medium text-ink-soft">#</th>
                <th className="px-3 py-2 font-medium text-ink-soft">Receta</th>
                <th className="px-3 py-2 font-medium text-ink-soft">PT</th>
                <th className="px-3 py-2 font-medium text-ink-soft">Planif.</th>
                <th className="px-3 py-2 font-medium text-ink-soft">Produc.</th>
                <th className="px-3 py-2 font-medium text-ink-soft">Fecha</th>
                <th className="px-3 py-2 font-medium text-ink-soft">Estado</th>
                <th className="px-3 py-2 font-medium text-ink-soft">Lote</th>
                <th className="px-3 py-2 font-medium text-ink-soft">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {orders.map((o) => (
                <tr key={o.id} className="border-b hover:bg-surface/50">
                  <td className="px-3 py-2">{o.id}</td>
                  <td className="px-3 py-2">{o.recipeNombre || `#${o.recipeId}`}</td>
                  <td className="px-3 py-2">{o.productoTerminadoNombre || '-'}</td>
                  <td className="px-3 py-2">{o.cantidadPlanificada}</td>
                  <td className="px-3 py-2">{o.cantidadProducida ?? '-'}</td>
                  <td className="px-3 py-2">{o.fechaPlanificada}</td>
                  <td className="px-3 py-2">{estadoBadge(o.estado)}</td>
                  <td className="px-3 py-2 text-xs">{o.numeroLote || '-'}</td>
                  <td className="px-3 py-2">
                    <div className="flex gap-1">
                      <button onClick={() => navigate(`/production-orders/${o.id}`)} className="rounded bg-blue-600 px-2 py-1 text-xs text-white hover:bg-blue-700">Ver</button>
                      {o.estado === 'PLANIFICADA' && (
                        <button onClick={() => handleStart(o.id)} className="rounded bg-green-600 px-2 py-1 text-xs text-white hover:bg-green-700">Iniciar</button>
                      )}
                      {(o.estado === 'PLANIFICADA' || o.estado === 'EN_PROCESO') && (
                        <button onClick={() => handleCancel(o.id)} className="rounded bg-red-600 px-2 py-1 text-xs text-white hover:bg-red-700">Cancelar</button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
              {orders.length === 0 && (
                <tr><td colSpan={9} className="px-3 py-8 text-center text-ink-faint">Sin ordenes de produccion</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </Layout>
  );
}


