import { useState, useEffect } from 'react';
import Layout from '../components/common/Layout';
import { branchApi } from '../services/api';
import { productApi } from '../services/api';
import { productPriceApi } from '../services/api';
import { branchPriceApi } from '../services/api';
import type {
  BranchResponse,
  PriceResolutionResponse,
  BranchPriceResponse,
  BranchPriceRequest,
  ProductResponse,
} from '../types';

export default function PricesPage() {
  const [branches, setBranches] = useState<BranchResponse[]>([]);
  const [selectedBranchId, setSelectedBranchId] = useState<number | null>(null);
  const [branchPrices, setBranchPrices] = useState<BranchPriceResponse[]>([]);
  const [products, setProducts] = useState<ProductResponse[]>([]);
  const [priceResolution, setPriceResolution] = useState<PriceResolutionResponse | null>(null);
  const [productToResolve, setProductToResolve] = useState<number | null>(null);

  const [loading, setLoading] = useState(false);
  const [branchPricesLoading, setBranchPricesLoading] = useState(false);
  const [error, setError] = useState('');

  const [openForm, setOpenForm] = useState(false);
  const [editing, setEditing] = useState(false);
  const [currentPrice, setCurrentPrice] = useState<BranchPriceResponse | null>(null);

  const [form, setForm] = useState<BranchPriceRequest>({
    productId: 0,
    precio: 0,
  });

  useEffect(() => {
    loadBranches();
    loadProducts();
  }, []);

  const loadBranches = async () => {
    try {
      const { data } = await branchApi.listAll();
      setBranches(data);
    } catch {
      // Ignorar error de sucursales
    }
  };

  const loadProducts = async () => {
    try {
      const { data } = await productApi.getAll();
      setProducts(data.content || data);
    } catch {
      // Ignorar error de productos
    }
  };

  const loadBranchPrices = async (branchId: number) => {
    setBranchPricesLoading(true);
    try {
      const { data } = await branchPriceApi.listByBranch(branchId);
      setBranchPrices(data);
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Error al cargar precios de sucursal';
      setError(msg);
    } finally {
      setBranchPricesLoading(false);
    }
  };

  const handleBranchChange = (branchId: number) => {
    setSelectedBranchId(branchId);
    loadBranchPrices(branchId);
  };

  const handleInputChange = (key: keyof BranchPriceRequest, value: string | number) => {
    setForm({ ...form, [key]: value });
  };

  const handleOpenCreate = () => {
    setEditing(false);
    setCurrentPrice(null);
    setForm({ productId: 0, precio: 0 });
    setOpenForm(true);
  };

  const handleOpenEdit = (price: BranchPriceResponse) => {
    setEditing(true);
    setCurrentPrice(price);
    setForm({
      productId: price.productId,
      precio: price.precio,
    });
    setOpenForm(true);
  };

  const handleSave = async () => {
    setError('');
    if (!selectedBranchId) {
      setError('Selecciona una sucursal primero');
      return;
    }
    try {
      if (editing && currentPrice) {
        await branchPriceApi.update(selectedBranchId, currentPrice.id, form);
      } else {
        await branchPriceApi.create(selectedBranchId, form);
      }
      setOpenForm(false);
      setEditing(false);
      await loadBranchPrices(selectedBranchId);
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Error al guardar precio';
      setError(msg);
    }
  };

  const handleDelete = async (priceId: number) => {
    setError('');
    if (!selectedBranchId) return;
    if (!window.confirm('¿Estás seguro de eliminar este precio local?')) return;
    try {
      await branchPriceApi.delete(selectedBranchId, priceId);
      await loadBranchPrices(selectedBranchId);
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Error al eliminar precio';
      setError(msg);
    }
  };

  const handleSyncGlobal = async () => {
    setError('');
    if (!selectedBranchId) {
      setError('Selecciona una sucursal primero');
      return;
    }
    try {
      await branchPriceApi.syncGlobal(selectedBranchId);
      await loadBranchPrices(selectedBranchId);
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Error al sincronizar precios';
      setError(msg);
    }
  };

  const handleResolvePrice = async (productId: number) => {
    setError('');
    setProductToResolve(productId);
    try {
      const { data } = await productPriceApi.resolvePrice(productId);
      setPriceResolution(data);
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Error al resolver precio';
      setError(msg);
    }
  };

  const getProductName = (productId: number) => {
    const p = products.find((p) => p.id === productId);
    return p ? p.name : `Producto #${productId}`;
  };

  if (loading) {
    return (
      <Layout title="Precios">
        <div className="flex h-full items-center justify-center text-ink-faint">
          Cargando datos…
        </div>
      </Layout>
    );
  }

  return (
    <Layout title="Precios por Sucursal">
      <div className="space-y-6">
        {/* Encabezado */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-ink">Precios por Sucursal</h1>
            <p className="text-sm text-ink-faint">
              Gestiona los precios locales diferenciados por sucursal de tu red Nivex.
            </p>
          </div>
        </div>

        {error && (
          <div className="rounded-md border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger">
            {error}
          </div>
        )}

        {/* Selector de sucursal */}
        <div className="rounded-xl border border-surface-raised bg-white/80 shadow-sm p-6">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div>
              <label className="mb-2 block text-sm font-medium text-ink-soft">
                Seleccionar Sucursal
              </label>
              <select
                value={selectedBranchId ?? ''}
                onChange={(e) => handleBranchChange(Number(e.target.value))}
                className="w-full md:w-72 rounded-md border border-surface-raised bg-white px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
              >
                <option value="">-- Seleccionar sucursal --</option>
                {branches.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.nombre}
                  </option>
                ))}
              </select>
            </div>

            {selectedBranchId && (
              <div className="flex gap-3">
                <button
                  onClick={handleSyncGlobal}
                  disabled={branchPricesLoading}
                  className="rounded-md bg-accent px-4 py-2.5 text-sm font-medium text-ink-on-dark transition-colors disabled:opacity-50 hover:bg-accent/90"
                >
                  {branchPricesLoading ? 'Sincronizando…' : 'Sincronizar con Global'}
                </button>
                <button
                  onClick={handleOpenCreate}
                  className="rounded-md bg-primary px-4 py-2.5 text-sm font-medium text-ink-on-dark transition-colors hover:bg-primary-soft"
                >
                  + Nuevo Precio Local
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Tabla de precios locales de la sucursal */}
        {selectedBranchId && (
          <div className="rounded-xl border border-surface-raised bg-white/80 shadow-sm">
            <div className="p-4 border-b border-surface-raised bg-surface-raised/50">
              <h2 className="text-lg font-bold text-ink">
                Precios Locales — {branches.find((b) => b.id === selectedBranchId)?.nombre}
              </h2>
            </div>

            <table className="w-full text-sm text-left border-collapse">
              <thead>
                <tr className="border-b border-surface-raised bg-surface-raised/50 text-ink-soft">
                  <th className="p-4 font-medium text-ink">SKU</th>
                  <th className="p-4 font-medium text-ink">Producto</th>
                  <th className="p-4 font-medium text-ink-soft text-right">Precio Local</th>
                  <th className="p-4 font-medium text-ink-soft text-right">Precio Global</th>
                  <th className="p-4 font-medium text-ink-soft text-center">Vigencia Desde</th>
                  <th className="p-4 font-medium text-ink-soft text-center">Vigencia Hasta</th>
                  <th className="p-4 font-medium text-ink-soft text-center">Estado</th>
                  <th className="p-4 font-medium text-ink-soft text-right">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {branchPrices.map((bp) => (
                  <tr key={bp.id} className="border-b border-surface-raised/50 hover:bg-surface-raised/30 transition-colors">
                    <td className="p-4 text-ink font-medium">{bp.productSku}</td>
                    <td className="p-4 text-ink">{bp.productName}</td>
                    <td className="p-4 text-right text-ink font-medium">
                      ${bp.precio.toFixed(2)}
                    </td>
                    <td className="p-4 text-right text-ink-soft">
                      ${bp.precioGlobal.toFixed(2)}
                    </td>
                    <td className="p-4 text-center text-ink-soft">
                      {bp.vigenciaDesde || '—'}
                    </td>
                    <td className="p-4 text-center text-ink-soft">
                      {bp.vigenciaHasta || '—'}
                    </td>
                    <td className="p-4 text-center">
                      <span
                        className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${
                          bp.activo
                            ? 'bg-success/10 text-success border border-success/20'
                            : 'bg-tertiary/10 text-tertiary border border-tertiary/20'
                        }`}
                      >
                        <span
                          className={`w-1.5 h-1.5 rounded-full ${bp.activo ? 'bg-success' : 'bg-tertiary'}`}
                          aria-hidden="true"
                        />
                        {bp.activo ? 'Activo' : 'Inactivo'}
                      </span>
                    </td>
                    <td className="p-4 text-right">
                      <button
                        onClick={() => handleOpenEdit(bp)}
                        className="text-accent hover:text-primary-soft transition-colors"
                        title="Editar"
                      >
                        Editar
                      </button>
                      <button
                        onClick={() => handleDelete(bp.id)}
                        className="ml-3 text-ink-faint hover:text-danger transition-colors"
                        title="Eliminar"
                      >
                        Eliminar
                      </button>
                    </td>
                  </tr>
                ))}
                {branchPrices.length === 0 && (
                  <tr>
                    <td colSpan={8} className="p-8 text-center text-ink-faint">
                      No hay precios locales para esta sucursal. Crea uno haciendo clic en "Nuevo Precio Local".
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}

        {/* Resolución de precio (precio final según sucursal del JWT) */}
        <div className="rounded-xl border border-surface-raised bg-white/80 shadow-sm p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-bold text-ink">Resolver Precio de Producto</h2>
            <p className="text-sm text-ink-faint">
              Consulta el precio vigente que se aplicaría en el POS según la sucursal activa del usuario.
            </p>
          </div>

          <div className="flex flex-col sm:flex-row gap-4">
            <div className="flex-1">
              <label className="mb-2 block text-sm font-medium text-ink-soft">
                Seleccionar Producto
              </label>
              <select
                value={productToResolve ?? ''}
                onChange={(e) => handleResolvePrice(Number(e.target.value))}
                className="w-full rounded-md border border-surface-raised bg-white px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
              >
                <option value="">-- Seleccionar producto --</option>
                {products.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.sku} — {p.name}
                  </option>
                ))}
              </select>
            </div>

            {priceResolution && (
              <div className="flex-1 p-4 rounded-lg border border-accent/20 bg-accent/5">
                <div className="flex justify-between items-end">
                  <div>
                    <p className="text-xs text-ink-faint uppercase tracking-wider">Precio Final</p>
                    <p className="text-2xl font-bold text-ink">
                      ${priceResolution.precioFinal.toFixed(2)}
                    </p>
                    <p className="text-xs mt-1">
                      {priceResolution.esLocal
                        ? 'Precio local de sucursal aplicado'
                        : 'Usando precio global (fallback)'}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-xs text-ink-faint uppercase tracking-wider">Producto</p>
                    <p className="text-sm font-medium text-ink">
                      {priceResolution.productSku} — {priceResolution.productName}
                    </p>
                    <p className="text-xs text-ink-faint">
                      Sucursal: {priceResolution.sucursalId || '—'}
                    </p>
                  </div>
                </div>
                <div className="mt-4 flex gap-4 text-xs text-ink-soft">
                  <span>
                    Precio Global: <span className="text-ink">${priceResolution.precioGlobal.toFixed(2)}</span>
                  </span>
                  {priceResolution.precioLocal !== null && (
                    <span>
                      Precio Local: <span className="text-ink">${priceResolution.precioLocal.toFixed(2)}</span>
                    </span>
                  )}
                  {priceResolution.vigenciaHasta && (
                    <span>
                      Vigencia Hasta: <span className="text-ink">{priceResolution.vigenciaHasta}</span>
                    </span>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Modal formulario de precio local */}
        {openForm && selectedBranchId && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <div
              className="absolute inset-0 bg-black/40 backdrop-blur-sm"
              onClick={() => setOpenForm(false)}
              aria-hidden="true"
            />
            <div className="relative w-full max-w-md rounded-xl bg-white p-6 shadow-lg border border-surface-raised">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-bold text-ink">
                  {editing ? 'Editar Precio Local' : 'Nuevo Precio Local'}
                </h2>
                <button
                  onClick={() => setOpenForm(false)}
                  className="rounded-md p-2 text-ink-faint hover:text-ink hover:bg-surface-raised transition-colors"
                  aria-label="Cerrar"
                >
                  <span aria-hidden="true">✕</span>
                </button>
              </div>

              <form onSubmit={(e) => { e.preventDefault(); handleSave(); }} className="space-y-4">
                <div>
                  <label className="mb-1 block text-sm font-medium text-ink-soft">
                    Producto *
                  </label>
                  <select
                    value={form.productId}
                    onChange={(e) => handleInputChange('productId', Number(e.target.value))}
                    required
                    className="w-full rounded-md border border-surface-raised bg-white px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                  >
                    <option value="0">-- Seleccionar producto --</option>
                    {products.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.sku} — {p.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-ink-soft">
                    Precio Local *
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    value={form.precio}
                    onChange={(e) => handleInputChange('precio', Number(e.target.value) || 0)}
                    required
                    className="w-full rounded-md border border-surface-raised bg-white px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="mb-1 block text-sm font-medium text-ink-soft">
                      Vigencia Desde
                    </label>
                    <input
                      type="date"
                      value={form.vigenciaDesde ?? ''}
                      onChange={(e) => handleInputChange('vigenciaDesde', e.target.value)}
                      className="w-full rounded-md border border-surface-raised bg-white px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-sm font-medium text-ink-soft">
                      Vigencia Hasta
                    </label>
                    <input
                      type="date"
                      value={form.vigenciaHasta ?? ''}
                      onChange={(e) => handleInputChange('vigenciaHasta', e.target.value)}
                      className="w-full rounded-md border border-surface-raised bg-white px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                    />
                  </div>
                </div>

                {error && (
                  <div className="rounded-md border border-danger/30 bg-danger/10 px-3 py-2 text-sm text-danger">
                    {error}
                  </div>
                )}

                <div className="flex justify-end gap-3 pt-4">
                  <button
                    type="button"
                    onClick={() => setOpenForm(false)}
                    className="rounded-md px-4 py-2 text-sm font-medium text-ink-soft bg-white border border-surface-raised hover:bg-surface-raised transition-colors"
                  >
                    Cancelar
                  </button>
                  <button
                    type="submit"
                    className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-ink-on-dark transition-colors hover:bg-primary-soft"
                  >
                    {editing ? 'Guardar cambios' : 'Crear Precio Local'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
}
