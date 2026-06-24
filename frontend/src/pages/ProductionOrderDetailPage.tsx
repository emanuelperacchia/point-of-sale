import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { productionOrderApi, lotApi } from '../services/api';
import type { ProductionOrderResponse, ProductionOrderComponentResponse, CostAnalysisResponse, LoteTraceabilityResponse } from '../types';
import Layout from '../components/common/Layout';

type MermaEntry = { bomComponentId: number; cantidad: number; motivo: string };

export default function ProductionOrderDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [order, setOrder] = useState<ProductionOrderResponse | null>(null);
  const [costAnalysis, setCostAnalysis] = useState<CostAnalysisResponse | null>(null);
  const [traceability, setTraceability] = useState<LoteTraceabilityResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Complete modal
  const [showComplete, setShowComplete] = useState(false);
  const [cantidadProducida, setCantidadProducida] = useState('');
  const [mermaEntries, setMermaEntries] = useState<Record<number, { cantidad: string; motivo: string }>>({});

  // Cost analysis toggle
  const [showCost, setShowCost] = useState(false);

  useEffect(() => {
    if (id) loadOrder();
  }, [id]);

  function loadOrder() {
    setLoading(true);
    setError('');
    productionOrderApi
      .getById(Number(id))
      .then((res) => {
        setOrder(res.data);
        // reset merma form defaults
        if (res.data.componentes) {
          const defaults: Record<number, { cantidad: string; motivo: string }> = {};
          res.data.componentes.forEach((c: ProductionOrderComponentResponse) => {
            defaults[c.id] = { cantidad: '', motivo: 'PROCESO' };
          });
          setMermaEntries(defaults);
        }
      })
      .catch(() => setError('Error al cargar orden'))
      .finally(() => setLoading(false));
  }

  function loadCostAnalysis() {
    if (costAnalysis) {
      setShowCost(!showCost);
      return;
    }
    productionOrderApi
      .getCostAnalysis(Number(id))
      .then((res) => {
        setCostAnalysis(res.data);
        setShowCost(true);
      })
      .catch(() => setError('Error al cargar analisis de costos'));
  }

  function loadTraceability() {
    if (traceability) return;
    if (order?.numeroLote) {
      lotApi
        .getTraceability(order.numeroLote)
        .then((res) => setTraceability(res.data))
        .catch(() => setError('Error al cargar trazabilidad'));
    }
  }

  function handleStart() {
    productionOrderApi
      .start(Number(id))
      .then(() => {
        setSuccess('Orden iniciada');
        loadOrder();
      })
      .catch((err) => setError(err.response?.data?.message || 'Error al iniciar'));
  }

  function handleComplete(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setSuccess('');

    const entries: MermaEntry[] = [];
    if (order) {
      order.componentes.forEach((c) => {
        const me = mermaEntries[c.id];
        if (me && me.cantidad) {
          entries.push({
            bomComponentId: c.id,
            cantidad: Number(me.cantidad),
            motivo: me.motivo,
          });
        }
      });
    }

    productionOrderApi
      .complete(Number(id), Number(cantidadProducida), entries.length > 0 ? entries : undefined)
      .then(() => {
        setSuccess('Orden completada');
        setShowComplete(false);
        loadOrder();
      })
      .catch((err) => setError(err.response?.data?.message || 'Error al completar'));
  }

  function handleCancel() {
    if (!confirm('Cancelar orden de produccion?')) return;
    productionOrderApi
      .cancel(Number(id))
      .then(() => {
        setSuccess('Orden cancelada');
        loadOrder();
      })
      .catch((err) => setError(err.response?.data?.message || 'Error al cancelar'));
  }

  function estadoBadge(estado: string) {
    const colors: Record<string, string> = {
      PLANIFICADA: 'bg-blue-100 text-blue-700',
      EN_PROCESO: 'bg-yellow-100 text-yellow-700',
      COMPLETADA: 'bg-green-100 text-green-700',
      CANCELADA: 'bg-red-100 text-red-700',
    };
    return <span className={`rounded px-2 py-0.5 text-xs font-medium ${colors[estado] || 'bg-gray-100 text-gray-700'}`}>{estado}</span>;
  }

  if (loading) {
    return (
      <Layout title="Orden de Produccion">
        <p className="text-gray-500">Cargando...</p>
      </Layout>
    );
  }

  if (!order) {
    return (
      <Layout title="Orden de Produccion">
        <div className="rounded border border-red-300 bg-red-50 px-4 py-2 text-sm text-red-700">{error || 'Orden no encontrada'}</div>
        <button onClick={() => navigate('/production-orders')} className="mt-2 rounded bg-gray-600 px-4 py-1.5 text-sm text-white hover:bg-gray-700">Volver</button>
      </Layout>
    );
  }

  return (
    <Layout title={`OP #${order.id} — ${order.recipeNombre || ''}`}>
      <div className="space-y-4">
        {error && <div className="rounded border border-red-300 bg-red-50 px-4 py-2 text-sm text-red-700">{error}</div>}
        {success && <div className="rounded border border-green-300 bg-green-50 px-4 py-2 text-sm text-green-700">{success}</div>}

        {/* ── Header + Actions ────────────────────────────────────── */}
        <div className="flex items-center justify-between rounded-lg bg-white p-4 shadow">
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-lg font-semibold text-gray-800">OP #{order.id}</h3>
              {estadoBadge(order.estado)}
            </div>
            <p className="mt-1 text-sm text-gray-500">
              {order.recipeNombre || `Receta #${order.recipeId}`}
              {order.productoTerminadoNombre ? ` → ${order.productoTerminadoNombre}` : ''}
            </p>
            {order.numeroLote && <p className="text-xs text-gray-400">Lote: {order.numeroLote}</p>}
          </div>
          <div className="flex gap-2">
            <button onClick={() => navigate('/production-orders')} className="rounded border border-gray-300 px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-50">Volver</button>
            {order.estado === 'PLANIFICADA' && (
              <button onClick={handleStart} className="rounded bg-green-600 px-3 py-1.5 text-sm text-white hover:bg-green-700">Iniciar</button>
            )}
            {order.estado === 'EN_PROCESO' && (
              <button onClick={() => setShowComplete(true)} className="rounded bg-blue-600 px-3 py-1.5 text-sm text-white hover:bg-blue-700">Completar</button>
            )}
            {(order.estado === 'PLANIFICADA' || order.estado === 'EN_PROCESO') && (
              <button onClick={handleCancel} className="rounded bg-red-600 px-3 py-1.5 text-sm text-white hover:bg-red-700">Cancelar</button>
            )}
          </div>
        </div>

        {/* ── Detail info ─────────────────────────────────────────── */}
        <div className="grid grid-cols-2 gap-4 rounded-lg bg-white p-4 shadow md:grid-cols-4">
          <div>
            <p className="text-xs text-gray-500">Cant. Planificada</p>
            <p className="text-sm font-medium">{order.cantidadPlanificada}</p>
          </div>
          <div>
            <p className="text-xs text-gray-500">Cant. Producida</p>
            <p className="text-sm font-medium">{order.cantidadProducida ?? '-'}</p>
          </div>
          <div>
            <p className="text-xs text-gray-500">Fecha Planificada</p>
            <p className="text-sm font-medium">{order.fechaPlanificada}</p>
          </div>
          <div>
            <p className="text-xs text-gray-500">Responsable</p>
            <p className="text-sm font-medium">{order.responsableNombre || `#${order.responsableId}`}</p>
          </div>
          {order.observaciones && (
            <div className="col-span-2">
              <p className="text-xs text-gray-500">Observaciones</p>
              <p className="text-sm">{order.observaciones}</p>
            </div>
          )}
        </div>

        {/* ── Components table ────────────────────────────────────── */}
        <div className="rounded-lg bg-white shadow">
          <div className="border-b px-4 py-2">
            <h4 className="text-sm font-semibold text-gray-700">Componentes</h4>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b bg-gray-50">
                  <th className="px-3 py-2 font-medium text-gray-600">Componente</th>
                  <th className="px-3 py-2 font-medium text-gray-600">Planif.</th>
                  <th className="px-3 py-2 font-medium text-gray-600">Consumido</th>
                  <th className="px-3 py-2 font-medium text-gray-600">Merma</th>
                  <th className="px-3 py-2 font-medium text-gray-600">Motivo</th>
                </tr>
              </thead>
              <tbody>
                {order.componentes.map((c) => (
                  <tr key={c.id} className="border-b hover:bg-gray-50">
                    <td className="px-3 py-2">{c.componenteNombre || `#${c.componenteId}`}</td>
                    <td className="px-3 py-2">{c.cantidadPlanificada}</td>
                    <td className="px-3 py-2">{c.cantidadConsumida ?? '-'}</td>
                    <td className="px-3 py-2">{c.mermaReal != null ? c.mermaReal : '-'}</td>
                    <td className="px-3 py-2 text-xs">{c.motivoMerma || '-'}</td>
                  </tr>
                ))}
                {order.componentes.length === 0 && (
                  <tr><td colSpan={5} className="px-3 py-8 text-center text-gray-400">Sin componentes</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* ── Complete modal ──────────────────────────────────────── */}
        {showComplete && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
            <div className="w-full max-w-lg rounded-lg bg-white p-6 shadow-xl">
              <h4 className="mb-4 text-base font-semibold">Completar Orden #{order.id}</h4>
              <form onSubmit={handleComplete} className="space-y-4">
                <div>
                  <label className="block text-xs text-gray-600">Cantidad Producida</label>
                  <input
                    type="number"
                    min={0}
                    value={cantidadProducida}
                    onChange={(e) => setCantidadProducida(e.target.value)}
                    required
                    className="mt-1 w-full rounded border px-3 py-1.5 text-sm"
                  />
                </div>
                {order.componentes
                  .filter((c) => c.mermaReal == null)
                  .map((c) => (
                    <div key={c.id} className="rounded bg-gray-50 p-3">
                      <p className="mb-1 text-sm font-medium">{c.componenteNombre || `#${c.componenteId}`}</p>
                      <div className="flex gap-2">
                        <div className="flex-1">
                          <label className="block text-xs text-gray-500">Merma real</label>
                          <input
                            type="number"
                            min={0}
                            placeholder="0"
                            value={mermaEntries[c.id]?.cantidad || ''}
                            onChange={(e) =>
                              setMermaEntries((prev) => ({
                                ...prev,
                                [c.id]: { ...prev[c.id], cantidad: e.target.value },
                              }))
                            }
                            className="mt-1 w-full rounded border px-2 py-1 text-sm"
                          />
                        </div>
                        <div className="flex-1">
                          <label className="block text-xs text-gray-500">Motivo</label>
                          <select
                            value={mermaEntries[c.id]?.motivo || 'PROCESO'}
                            onChange={(e) =>
                              setMermaEntries((prev) => ({
                                ...prev,
                                [c.id]: { ...prev[c.id], motivo: e.target.value },
                              }))
                            }
                            className="mt-1 w-full rounded border px-2 py-1 text-sm"
                          >
                            <option value="PROCESO">Proceso</option>
                            <option value="VENCIMIENTO">Vencimiento</option>
                            <option value="DEFECTO">Defecto</option>
                            <option value="OTRO">Otro</option>
                          </select>
                        </div>
                      </div>
                    </div>
                  ))}
                <div className="flex justify-end gap-2">
                  <button type="button" onClick={() => setShowComplete(false)} className="rounded border border-gray-300 px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-50">Cancelar</button>
                  <button type="submit" className="rounded bg-blue-600 px-4 py-1.5 text-sm text-white hover:bg-blue-700">Confirmar</button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* ── Cost Analysis ───────────────────────────────────────── */}
        {order.estado === 'COMPLETADA' && (
          <div className="rounded-lg bg-white shadow">
            <button
              onClick={loadCostAnalysis}
              className="flex w-full items-center justify-between px-4 py-2 text-left"
            >
              <span className="text-sm font-semibold text-gray-700">Analisis de Costos</span>
              <span className="text-xs text-gray-400">{showCost ? 'Ocultar' : 'Mostrar'}</span>
            </button>
            {showCost && costAnalysis && (
              <div className="border-t px-4 py-3">
                <div className="mb-3 grid grid-cols-3 gap-4 text-sm">
                  <div>
                    <p className="text-xs text-gray-500">Costo Estimado</p>
                    <p className="font-medium text-gray-800">${costAnalysis.costoEstimadoTotal.toFixed(2)}</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-500">Costo Real</p>
                    <p className="font-medium text-gray-800">${costAnalysis.costoRealTotal.toFixed(2)}</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-500">Desviacion</p>
                    <p className={`font-medium ${costAnalysis.desviacion >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                      {costAnalysis.desviacion >= 0 ? '+' : ''}{costAnalysis.desviacion.toFixed(2)}%
                    </p>
                  </div>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-sm">
                    <thead>
                      <tr className="border-b bg-gray-50">
                        <th className="px-2 py-1 font-medium text-gray-600">Componente</th>
                        <th className="px-2 py-1 font-medium text-gray-600">Planif.</th>
                        <th className="px-2 py-1 font-medium text-gray-600">Consumido</th>
                        <th className="px-2 py-1 font-medium text-gray-600">P.U.</th>
                        <th className="px-2 py-1 font-medium text-gray-600">Est.</th>
                        <th className="px-2 py-1 font-medium text-gray-600">Real</th>
                        <th className="px-2 py-1 font-medium text-gray-600">Desv.</th>
                      </tr>
                    </thead>
                    <tbody>
                      {costAnalysis.componentes.map((comp, i) => (
                        <tr key={i} className="border-b hover:bg-gray-50">
                          <td className="px-2 py-1">{comp.componenteNombre}</td>
                          <td className="px-2 py-1">{comp.cantidadPlanificada}</td>
                          <td className="px-2 py-1">{comp.cantidadConsumida}</td>
                          <td className="px-2 py-1">${comp.precioUnitario.toFixed(2)}</td>
                          <td className="px-2 py-1">${comp.costoEstimado.toFixed(2)}</td>
                          <td className="px-2 py-1">${comp.costoReal.toFixed(2)}</td>
                          <td className={`px-2 py-1 ${comp.desviacion >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                            {comp.desviacion >= 0 ? '+' : ''}{comp.desviacion.toFixed(2)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>
        )}

        {/* ── Lot Traceability ────────────────────────────────────── */}
        {order.estado === 'COMPLETADA' && order.numeroLote && (
          <div className="rounded-lg bg-white shadow">
            <button
              onClick={loadTraceability}
              className="flex w-full items-center justify-between px-4 py-2 text-left"
            >
              <span className="text-sm font-semibold text-gray-700">
                Trazabilidad — Lote {order.numeroLote}
              </span>
              <span className="text-xs text-gray-400">{traceability ? 'Ocultar' : 'Mostrar'}</span>
            </button>
            {traceability && (
              <div className="border-t px-4 py-3">
                <div className="mb-3 text-sm">
                  <p><span className="text-gray-500">Fecha produccion:</span> {traceability.fechaProduccion}</p>
                  <p><span className="text-gray-500">Cantidad:</span> {traceability.cantidad}</p>
                  <p><span className="text-gray-500">Producto:</span> {traceability.productoTerminadoNombre || '-'}</p>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-sm">
                    <thead>
                      <tr className="border-b bg-gray-50">
                        <th className="px-2 py-1 font-medium text-gray-600">Materia Prima</th>
                        <th className="px-2 py-1 font-medium text-gray-600">SKU</th>
                        <th className="px-2 py-1 font-medium text-gray-600">Cantidad</th>
                        <th className="px-2 py-1 font-medium text-gray-600">Lote Compra</th>
                        <th className="px-2 py-1 font-medium text-gray-600">Proveedor</th>
                      </tr>
                    </thead>
                    <tbody>
                      {traceability.materiasPrimas.map((mp, i) => (
                        <tr key={i} className="border-b hover:bg-gray-50">
                          <td className="px-2 py-1">{mp.productoNombre}</td>
                          <td className="px-2 py-1 text-xs">{mp.productoSku}</td>
                          <td className="px-2 py-1">{mp.cantidad}</td>
                          <td className="px-2 py-1 text-xs">{mp.loteCompra}</td>
                          <td className="px-2 py-1">{mp.proveedor}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </Layout>
  );
}
