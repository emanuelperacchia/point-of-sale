import { useState, useEffect } from 'react';
import Layout from '../components/common/Layout';
import { productApi } from '../services/api';
import { categoryApi } from '../services/api';
import type { ProductResponse, ProductCreateRequest, ProductUpdateRequest, ProductTipo, ProductCategory } from '../types';

export default function ProductsPage() {
  const [products, setProducts] = useState<ProductResponse[]>([]);
  const [categories, setCategories] = useState<ProductCategory[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [openForm, setOpenForm] = useState(false);
  const [editing, setEditing] = useState(false);
  const [currentProduct, setCurrentProduct] = useState<ProductResponse | null>(null);

  const [form, setForm] = useState<ProductCreateRequest>({
    sku: '',
    name: '',
    description: '',
    price: 0,
    stock: 0,
    tipo: 'PRODUCTO',
    categoryId: undefined,
  });

  const PRODUCT_TIPO_LABELS: Record<ProductTipo, string> = {
    PRODUCTO: 'Producto',
    SERVICIO: 'Servicio',
    MATERIA_PRIMA: 'Materia Prima',
  };

  useEffect(() => {
    loadProducts();
    loadCategories();
  }, []);

  const loadProducts = async () => {
    setLoading(true);
    setError('');
    try {
      const { data } = await productApi.getAll();
      setProducts(data.content || data);
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Error al cargar productos';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const loadCategories = async () => {
    try {
      const { data } = await categoryApi.getAllActive();
      setCategories(data);
    } catch {
      // Si falla cargar categorías, no bloqueamos la página
    }
  };

  const handleInputChange = (key: keyof ProductCreateRequest, value: string | number | boolean) => {
    setForm({ ...form, [key]: value });
  };

  const handleOpenCreate = () => {
    setEditing(false);
    setCurrentProduct(null);
    setForm({
      sku: '',
      name: '',
      description: '',
      price: 0,
      stock: 0,
      tipo: 'PRODUCTO',
      categoryId: undefined,
    });
    setOpenForm(true);
  };

  const handleOpenEdit = (product: ProductResponse) => {
    setEditing(true);
    setCurrentProduct(product);
    setForm({
      sku: product.sku,
      name: product.name,
      description: product.description ?? '',
      price: product.price,
      stock: product.stock,
      tipo: product.tipo,
      categoryId: product.category?.id,
    });
    setOpenForm(true);
  };

  const handleSave = async () => {
    setError('');
    try {
      if (editing && currentProduct) {
        await productApi.update(currentProduct.id, form as ProductUpdateRequest);
      } else {
        await productApi.create(form);
      }
      setOpenForm(false);
      setEditing(false);
      await loadProducts();
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Error al guardar producto';
      setError(msg);
    }
  };

  const handleToggleActive = async (product: ProductResponse) => {
    setError('');
    try {
      await productApi.update(product.id, { active: !product.active } as ProductUpdateRequest);
      await loadProducts();
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Error al actualizar estado de producto';
      setError(msg);
    }
  };

  const handleUpdateStock = async (product: ProductResponse, quantity: number) => {
    setError('');
    try {
      await productApi.updateStock(product.id, quantity);
      await loadProducts();
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Error al actualizar stock';
      setError(msg);
    }
  };

  if (loading) {
    return (
      <Layout title="Productos">
        <div className="flex h-full items-center justify-center text-ink-faint">
          Cargando productos…
        </div>
      </Layout>
    );
  }

  return (
    <Layout title="Productos">
      <div className="space-y-6">
        {/* Encabezado */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-ink">Productos</h1>
            <p className="text-sm text-ink-faint">
              Gestiona el catálogo de productos de tu red Nivex. SKU, precio, stock y categoría.
            </p>
          </div>
          <button
            onClick={handleOpenCreate}
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2.5 text-sm font-medium text-ink-on-dark transition-colors hover:bg-primary-soft"
          >
            <span aria-hidden="true">+</span> Nuevo producto
          </button>
        </div>

        {error && (
          <div className="rounded-md border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger">
            {error}
          </div>
        )}

        {/* Tabla de productos */}
        <div className="rounded-xl border border-surface-raised bg-surface/80 ">
          <table className="w-full text-sm text-left border-collapse">
            <thead>
              <tr className="border-b border-surface-raised bg-surface-raised/50 text-ink-soft">
                <th className="p-4 font-medium text-ink">SKU</th>
                <th className="p-4 font-medium text-ink">Nombre</th>
                <th className="p-4 font-medium text-ink-soft">Categoría</th>
                <th className="p-4 font-medium text-ink-soft text-right">Precio</th>
                <th className="p-4 font-medium text-ink-soft text-right">Stock</th>
                <th className="p-4 font-medium text-ink-soft text-center">Tipo</th>
                <th className="p-4 font-medium text-ink-soft text-center">Estado</th>
                <th className="p-4 font-medium text-ink-soft text-right">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {products.map((p) => (
                <tr key={p.id} className="border-b border-surface-raised/50 hover:bg-surface-raised/30 transition-colors">
                  <td className="p-4 text-ink font-medium">{p.sku}</td>
                  <td className="p-4 text-ink">{p.name}</td>
                  <td className="p-4 text-ink-soft">{p.category?.name || '—'}</td>
                  <td className="p-4 text-right text-ink-soft">${p.price.toFixed(2)}</td>
                  <td className="p-4 text-right text-ink-soft font-mono">{p.stock}</td>
                  <td className="p-4 text-center">
                    <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium bg-accent/10 text-accent border border-accent/20">
                      {PRODUCT_TIPO_LABELS[p.tipo]}
                    </span>
                  </td>
                  <td className="p-4 text-center">
                    <span
                      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${
                        p.active
                          ? 'bg-success/10 text-success border border-success/20'
                          : 'bg-tertiary/10 text-tertiary border border-tertiary/20'
                      }`}
                    >
                      <span
                        className={`w-1.5 h-1.5 rounded-full ${p.active ? 'bg-success' : 'bg-tertiary'}`}
                        aria-hidden="true"
                      />
                      {p.active ? 'Activo' : 'Inactivo'}
                    </span>
                  </td>
                  <td className="p-4 text-right">
                    <button
                      onClick={() => handleOpenEdit(p)}
                      className="text-accent hover:text-primary-soft transition-colors"
                      title="Editar"
                    >
                      Editar
                    </button>
                    <button
                      onClick={() => handleToggleActive(p)}
                      className="ml-1 text-ink-faint hover:text-danger transition-colors"
                      title={p.active ? 'Desactivar' : 'Activar'}
                    >
                      {p.active ? 'Desactivar' : 'Activar'}
                    </button>
                  </td>
                </tr>
              ))}
              {products.length === 0 && (
                <tr>
                  <td colSpan={8} className="p-8 text-center text-ink-faint">
                    No hay productos registrados. Crea el primero haciendo clic en "Nuevo producto".
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Modal formulario */}
        {openForm && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <div
              className="absolute inset-0 bg-black/40 backdrop-blur-sm"
              onClick={() => setOpenForm(false)}
              aria-hidden="true"
            />
            <div className="relative w-full max-w-md rounded-xl bg-surface p-6  border border-surface-raised">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-bold text-ink">
                  {editing ? 'Editar producto' : 'Nuevo producto'}
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
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="mb-1 block text-sm font-medium text-ink-soft">
                      SKU *
                    </label>
                    <input
                      type="text"
                      value={form.sku}
                      onChange={(e) => handleInputChange('sku', e.target.value)}
                      required
                      className="w-full rounded-md border border-surface-raised bg-surface px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                    />
                  </div>

                  <div className="md:col-span-2">
                    <label className="mb-1 block text-sm font-medium text-ink-soft">
                      Nombre *
                    </label>
                    <input
                      type="text"
                      value={form.name}
                      onChange={(e) => handleInputChange('name', e.target.value)}
                      required
                      className="w-full rounded-md border border-surface-raised bg-surface px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                    />
                  </div>

                  <div>
                    <label className="mb-1 block text-sm font-medium text-ink-soft">
                      Descripción
                    </label>
                    <textarea
                      value={form.description}
                      onChange={(e) => handleInputChange('description', e.target.value)}
                      rows={2}
                      className="w-full rounded-md border border-surface-raised bg-surface px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                    />
                  </div>

                  <div className="grid grid-cols-3 gap-4">
                    <div>
                      <label className="mb-1 block text-sm font-medium text-ink-soft">
                        Precio *
                      </label>
                      <input
                        type="number"
                        step="0.01"
                        value={form.price}
                        onChange={(e) => handleInputChange('price', Number(e.target.value) || 0)}
                        required
                        className="w-full rounded-md border border-surface-raised bg-surface px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                      />
                    </div>
                    <div>
                      <label className="mb-1 block text-sm font-medium text-ink-soft">
                        Stock *
                      </label>
                      <input
                        type="number"
                        step="1"
                        value={form.stock}
                        onChange={(e) => handleInputChange('stock', Number(e.target.value) || 0)}
                        required
                        className="w-full rounded-md border border-surface-raised bg-surface px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                      />
                    </div>
                    <div>
                      <label className="mb-1 block text-sm font-medium text-ink-soft">
                        Tipo
                      </label>
                      <select
                        value={form.tipo}
                        onChange={(e) => handleInputChange('tipo', e.target.value as ProductTipo)}
                        className="w-full rounded-md border border-surface-raised bg-surface px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                      >
                        {Object.entries(PRODUCT_TIPO_LABELS).map(([key, label]) => (
                          <option key={key} value={key as ProductTipo}>
                            {label}
                          </option>
                        ))}
                      </select>
                    </div>
                  </div>

                  <div>
                    <label className="mb-1 block text-sm font-medium text-ink-soft">
                      Categoría
                    </label>
                    <select
                      value={form.categoryId ?? ''}
                      onChange={(e) => {
                        const val = e.target.value;
                        handleInputChange('categoryId', val === '' ? undefined : Number(val));
                      }}
                      className="w-full rounded-md border border-surface-raised bg-surface px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                    >
                      <option value="">Sin categoría</option>
                      {categories.map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.name}
                        </option>
                      ))}
                    </select>
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
                    className="rounded-md px-4 py-2 text-sm font-medium text-ink-soft bg-surface border border-surface-raised hover:bg-surface-raised transition-colors"
                  >
                    Cancelar
                  </button>
                  <button
                    type="submit"
                    className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-ink-on-dark transition-colors hover:bg-primary-soft"
                  >
                    {editing ? 'Guardar cambios' : 'Crear producto'}
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

