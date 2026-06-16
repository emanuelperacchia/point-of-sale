import { useState, useEffect } from 'react';
import { recipeApi } from '../services/api';
import type { RecipeResponse, RecipeRequest, ProductResponse } from '../types';
import { productApi } from '../services/api';
import Layout from '../components/common/Layout';

export default function RecipePage() {
  const [recipes, setRecipes] = useState<RecipeResponse[]>([]);
  const [products, setProducts] = useState<ProductResponse[]>([]);
  const [explosion, setExplosion] = useState<any>(null);
  const [costEstimate, setCostEstimate] = useState<any>(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Form
  const [nombre, setNombre] = useState('');
  const [descripcion, setDescripcion] = useState('');
  const [ptId, setPtId] = useState('');
  const [cantidadProducida, setCantidadProducida] = useState('1');
  const [unidadMedida, setUnidadMedida] = useState('unidad');
  const [tiempo, setTiempo] = useState('');
  const [componentes, setComponentes] = useState<any[]>([]);

  // Explosion
  const [explosionRecipeId, setExplosionRecipeId] = useState('');
  const [explosionCantidad, setExplosionCantidad] = useState('1');

  useEffect(() => {
    loadRecipes();
    loadProducts();
  }, []);

  function loadRecipes() {
    recipeApi.list().then((res) => setRecipes(res.data)).catch(() => {});
  }

  function loadProducts() {
    productApi
      .getAll(0, 100, 'name', 'asc', undefined, 'PRODUCTO_TERMINADO')
      .then((res) => setProducts(res.data.content))
      .catch(() => {});
  }

  function addComponent() {
    setComponentes([...componentes, { componenteId: '', cantidad: '', unidadMedida: 'unidad', esMermaEsperada: false, porcentajeMermaEsperado: '' }]);
  }

  function updateComponent(idx: number, field: string, value: any) {
    const updated = [...componentes];
    (updated[idx] as any)[field] = value;
    setComponentes(updated);
  }

  function removeComponent(idx: number) {
    setComponentes(componentes.filter((_, i) => i !== idx));
  }

  function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setSuccess('');

    const payload: RecipeRequest = {
      nombre,
      descripcion: descripcion || undefined,
      productoTerminadoId: Number(ptId),
      cantidadProducida: Number(cantidadProducida),
      unidadMedida,
      tiempoProduccionMinutos: tiempo ? Number(tiempo) : undefined,
      componentes: componentes.map((c) => ({
        componenteId: Number(c.componenteId),
        cantidad: Number(c.cantidad),
        unidadMedida: c.unidadMedida,
        esMermaEsperada: c.esMermaEsperada,
        porcentajeMermaEsperado: c.porcentajeMermaEsperado ? Number(c.porcentajeMermaEsperado) : undefined,
      })),
    };

    recipeApi
      .create(payload)
      .then(() => {
        setSuccess('Receta creada exitosamente');
        setNombre('');
        setDescripcion('');
        setPtId('');
        setCantidadProducida('1');
        setTiempo('');
        setComponentes([]);
        loadRecipes();
      })
      .catch((err) => setError(err.response?.data?.message || 'Error al crear receta'));
  }

  function handleExplosion() {
    if (!explosionRecipeId) return;
    recipeApi
      .getBomExplosion(Number(explosionRecipeId), Number(explosionCantidad))
      .then((res) => setExplosion(res.data))
      .catch((err) => setError(err.response?.data?.message || 'Error al obtener explosion'));
  }

  function handleCostEstimate(id: number) {
    recipeApi
      .getCostEstimate(id, 1)
      .then((res) => setCostEstimate(res.data))
      .catch((err) => setError(err.response?.data?.message || 'Error al estimar costo'));
  }

  return (
    <Layout title="Recetas y BOM">
      <div className="space-y-6">
        {error && <div className="rounded border border-red-300 bg-red-50 px-4 py-2 text-sm text-red-700">{error}</div>}
        {success && <div className="rounded border border-green-300 bg-green-50 px-4 py-2 text-sm text-green-700">{success}</div>}

        {/* ── Crear Receta ────────────────────────────────────────── */}
        <section className="rounded-lg bg-white p-4 shadow">
          <h3 className="mb-3 text-base font-semibold text-gray-800">Nueva Receta</h3>
          <form onSubmit={handleCreate} className="space-y-3">
            <div className="flex flex-wrap gap-3">
              <div>
                <label className="block text-xs text-gray-600">Nombre</label>
                <input value={nombre} onChange={(e) => setNombre(e.target.value)} required className="mt-1 rounded border px-3 py-1.5 text-sm" />
              </div>
              <div>
                <label className="block text-xs text-gray-600">Producto Terminado</label>
                <select value={ptId} onChange={(e) => setPtId(e.target.value)} required className="mt-1 rounded border px-3 py-1.5 text-sm">
                  <option value="">Seleccionar...</option>
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>{p.name} ({p.sku})</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs text-gray-600">Cantidad Producida</label>
                <input type="number" step="0.01" value={cantidadProducida} onChange={(e) => setCantidadProducida(e.target.value)} required className="mt-1 w-20 rounded border px-3 py-1.5 text-sm" />
              </div>
              <div>
                <label className="block text-xs text-gray-600">Unidad</label>
                <input value={unidadMedida} onChange={(e) => setUnidadMedida(e.target.value)} required className="mt-1 w-24 rounded border px-3 py-1.5 text-sm" />
              </div>
              <div>
                <label className="block text-xs text-gray-600">Tiempo (min)</label>
                <input type="number" value={tiempo} onChange={(e) => setTiempo(e.target.value)} className="mt-1 w-20 rounded border px-3 py-1.5 text-sm" />
              </div>
            </div>
            <div>
              <label className="block text-xs text-gray-600">Descripcion</label>
              <textarea value={descripcion} onChange={(e) => setDescripcion(e.target.value)} className="mt-1 w-full rounded border px-3 py-1.5 text-sm" rows={2} />
            </div>

            {/* Componentes */}
            <div>
              <div className="flex items-center justify-between">
                <label className="text-xs font-medium text-gray-600">Componentes BOM</label>
                <button type="button" onClick={addComponent} className="text-xs text-blue-600 hover:text-blue-800">+ Agregar componente</button>
              </div>
              {componentes.map((c, idx) => (
                <div key={idx} className="mt-2 flex flex-wrap items-end gap-2 rounded bg-gray-50 p-2">
                  <div>
                    <label className="block text-xs text-gray-500">Producto</label>
                    <select value={c.componenteId} onChange={(e) => updateComponent(idx, 'componenteId', e.target.value)} required className="rounded border px-2 py-1 text-xs">
                      <option value="">Seleccionar</option>
                      {products.map((p) => (
                        <option key={p.id} value={p.id}>{p.name}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs text-gray-500">Cantidad</label>
                    <input type="number" step="0.0001" value={c.cantidad} onChange={(e) => updateComponent(idx, 'cantidad', e.target.value)} required className="w-20 rounded border px-2 py-1 text-xs" />
                  </div>
                  <div>
                    <label className="block text-xs text-gray-500">Unidad</label>
                    <input value={c.unidadMedida} onChange={(e) => updateComponent(idx, 'unidadMedida', e.target.value)} required className="w-16 rounded border px-2 py-1 text-xs" />
                  </div>
                  <div className="flex items-center gap-1">
                    <input type="checkbox" checked={c.esMermaEsperada} onChange={(e) => updateComponent(idx, 'esMermaEsperada', e.target.checked)} className="h-3 w-3" />
                    <label className="text-xs text-gray-500">Merma esperada</label>
                  </div>
                  {c.esMermaEsperada && (
                    <div>
                      <label className="block text-xs text-gray-500">% Merma</label>
                      <input type="number" step="0.01" value={c.porcentajeMermaEsperado} onChange={(e) => updateComponent(idx, 'porcentajeMermaEsperado', e.target.value)} className="w-16 rounded border px-2 py-1 text-xs" />
                    </div>
                  )}
                  <button type="button" onClick={() => removeComponent(idx)} className="text-xs text-red-500 hover:text-red-700">Quitar</button>
                </div>
              ))}
            </div>

            <button type="submit" className="rounded bg-blue-600 px-4 py-1.5 text-sm text-white hover:bg-blue-700">Crear Receta</button>
          </form>
        </section>

        {/* ── Lista ────────────────────────────────────────────────── */}
        <section className="rounded-lg bg-white p-4 shadow">
          <h3 className="mb-3 text-base font-semibold text-gray-800">Recetas</h3>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b bg-gray-50">
                  <th className="px-3 py-2 font-medium text-gray-600">Nombre</th>
                  <th className="px-3 py-2 font-medium text-gray-600">PT</th>
                  <th className="px-3 py-2 font-medium text-gray-600">Cantidad</th>
                  <th className="px-3 py-2 font-medium text-gray-600">Costo Est.</th>
                  <th className="px-3 py-2 font-medium text-gray-600">Componentes</th>
                  <th className="px-3 py-2 font-medium text-gray-600">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {recipes.map((r) => (
                  <tr key={r.id} className="border-b hover:bg-gray-50">
                    <td className="px-3 py-2">{r.nombre}</td>
                    <td className="px-3 py-2">{r.productoTerminadoNombre || `#${r.productoTerminadoId}`}</td>
                    <td className="px-3 py-2">{r.cantidadProducida} {r.unidadMedida}</td>
                    <td className="px-3 py-2">${r.costoEstimado?.toFixed(2)}</td>
                    <td className="px-3 py-2">{r.componentes?.length || 0}</td>
                    <td className="px-3 py-2">
                      <button onClick={() => handleCostEstimate(r.id)} className="rounded bg-purple-600 px-2 py-1 text-xs text-white hover:bg-purple-700">Costo</button>
                    </td>
                  </tr>
                ))}
                {recipes.length === 0 && <tr><td colSpan={6} className="px-3 py-4 text-center text-gray-400">Sin recetas</td></tr>}
              </tbody>
            </table>
          </div>
        </section>

        {/* ── Explosion BOM ────────────────────────────────────────── */}
        <section className="rounded-lg bg-white p-4 shadow">
          <h3 className="mb-3 text-base font-semibold text-gray-800">Explosion BOM</h3>
          <div className="mb-3 flex items-end gap-3">
            <div>
              <label className="block text-xs text-gray-600">Receta</label>
              <select value={explosionRecipeId} onChange={(e) => setExplosionRecipeId(e.target.value)} className="mt-1 rounded border px-3 py-1.5 text-sm">
                <option value="">Seleccionar...</option>
                {recipes.map((r) => (
                  <option key={r.id} value={r.id}>{r.nombre}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs text-gray-600">Cantidad</label>
              <input type="number" value={explosionCantidad} onChange={(e) => setExplosionCantidad(e.target.value)} className="mt-1 w-20 rounded border px-3 py-1.5 text-sm" />
            </div>
            <button onClick={handleExplosion} className="rounded bg-green-600 px-4 py-1.5 text-sm text-white hover:bg-green-700">Explotar</button>
          </div>

          {explosion && (
            <div>
              <p className="mb-2 text-sm text-gray-600">
                <strong>Receta:</strong> {explosion.recipeNombre} | <strong>Cantidad:</strong> {explosion.cantidadAProducir}
              </p>
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b bg-gray-50">
                    <th className="px-3 py-2 font-medium text-gray-600">Producto</th>
                    <th className="px-3 py-2 font-medium text-gray-600">Tipo</th>
                    <th className="px-3 py-2 font-medium text-gray-600">Cantidad</th>
                    <th className="px-3 py-2 font-medium text-gray-600">Costo</th>
                    <th className="px-3 py-2 font-medium text-gray-600">Stock</th>
                  </tr>
                </thead>
                <tbody>
                  {explosion.materiales?.map((m: any) => (
                    <tr key={m.productoId} className="border-b">
                      <td className="px-3 py-2">{m.productoNombre}</td>
                      <td className="px-3 py-2">{m.tipo}</td>
                      <td className="px-3 py-2">{m.cantidadTotal} {m.unidadMedida}</td>
                      <td className="px-3 py-2">${m.costoTotal?.toFixed(2)}</td>
                      <td className="px-3 py-2">
                        <span className={m.stockSuficiente ? 'text-green-600' : 'text-red-600'}>
                          {m.stockActual} {!m.stockSuficiente && `(faltan ${m.stockFaltante})`}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {/* ── Cost Estimate ──────────────────────────────────────── */}
        {costEstimate && (
          <section className="rounded-lg bg-white p-4 shadow">
            <h3 className="mb-3 text-base font-semibold text-gray-800">Costo Estimado</h3>
            <div className="grid grid-cols-3 gap-4 text-sm">
              <div><strong>Costo Total:</strong> ${costEstimate.costoTotalEstimado?.toFixed(2)}</div>
              <div><strong>Costo Unitario:</strong> ${costEstimate.costoUnitarioEstimado?.toFixed(2)}</div>
              <div><strong>Cantidad:</strong> {costEstimate.cantidadAProducir}</div>
            </div>
            <table className="mt-3 w-full text-left text-sm">
              <thead>
                <tr className="border-b bg-gray-50">
                  <th className="px-3 py-2 font-medium text-gray-600">Componente</th>
                  <th className="px-3 py-2 font-medium text-gray-600">Cantidad</th>
                  <th className="px-3 py-2 font-medium text-gray-600">Precio Unit.</th>
                  <th className="px-3 py-2 font-medium text-gray-600">Subtotal</th>
                </tr>
              </thead>
              <tbody>
                {costEstimate.items?.map((item: any) => (
                  <tr key={item.productoId} className="border-b">
                    <td className="px-3 py-2">{item.productoNombre}</td>
                    <td className="px-3 py-2">{item.cantidad}</td>
                    <td className="px-3 py-2">${item.precioUnitario?.toFixed(2)}</td>
                    <td className="px-3 py-2">${item.costoTotal?.toFixed(2)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>
        )}
      </div>
    </Layout>
  );
}
