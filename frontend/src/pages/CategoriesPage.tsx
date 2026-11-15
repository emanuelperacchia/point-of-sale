import { useState, useEffect } from 'react';
import Layout from '../components/common/Layout';
import { categoryApi } from '../services/api';
import type { CategoryResponse, CategoryRequest } from '../types';

export default function CategoriesPage() {
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [openForm, setOpenForm] = useState(false);
  const [editing, setEditing] = useState(false);
  const [currentCategory, setCurrentCategory] = useState<CategoryResponse | null>(null);

  const [form, setForm] = useState<CategoryRequest>({
    name: '',
    description: '',
    active: true,
  });

  useEffect(() => {
    loadCategories();
  }, []);

  const loadCategories = async () => {
    setLoading(true);
    setError('');
    try {
      const { data } = await categoryApi.getAllActive();
      setCategories(data);
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Error al cargar categorías';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (key: keyof CategoryRequest, value: string | boolean) => {
    setForm({ ...form, [key]: value });
  };

  const handleOpenCreate = () => {
    setEditing(false);
    setCurrentCategory(null);
    setForm({ name: '', description: '', active: true });
    setOpenForm(true);
  };

  const handleOpenEdit = (category: CategoryResponse) => {
    setEditing(true);
    setCurrentCategory(category);
    setForm({
      name: category.name,
      description: category.description ?? '',
      active: category.active,
    });
    setOpenForm(true);
  };

  const handleSave = async () => {
    setError('');
    try {
      if (editing && currentCategory) {
        await categoryApi.update(currentCategory.id, form);
      } else {
        await categoryApi.create(form);
      }
      setOpenForm(false);
      setEditing(false);
      await loadCategories();
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Error al guardar categoría';
      setError(msg);
    }
  };

  const handleToggleActive = async (category: CategoryResponse) => {
    setError('');
    try {
      await categoryApi.update(category.id, { ...category, active: !category.active });
      await loadCategories();
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Error al actualizar estado de categoría';
      setError(msg);
    }
  };

  if (loading) {
    return (
      <Layout title="Categorías">
        <div className="flex h-full items-center justify-center text-ink-faint">
          Cargando categorías…
        </div>
      </Layout>
    );
  }

  return (
    <Layout title="Categorías">
      <div className="space-y-6">
        {/* Encabezado */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-ink">Categorías</h1>
            <p className="text-sm text-ink-faint">
              Gestiona las categorías de productos de tu red Nivex. Úsalas para organizar tu catálogo.
            </p>
          </div>
          <button
            onClick={handleOpenCreate}
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2.5 text-sm font-medium text-ink-on-dark transition-colors hover:bg-primary-soft"
          >
            <span aria-hidden="true">+</span> Nueva categoría
          </button>
        </div>

        {error && (
          <div className="rounded-md border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger">
            {error}
          </div>
        )}

        {/* Tabla de categorías */}
        <div className="rounded-xl border border-surface-raised bg-surface/80 ">
          <table className="w-full text-sm text-left border-collapse">
            <thead>
              <tr className="border-b border-surface-raised bg-surface-raised/50 text-ink-soft">
                <th className="p-4 font-medium text-ink">Nombre</th>
                <th className="p-4 font-medium text-ink-soft">Descripción</th>
                <th className="p-4 font-medium text-ink-soft text-center">Estado</th>
                <th className="p-4 font-medium text-ink-soft text-right">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {categories.map((c) => (
                <tr key={c.id} className="border-b border-surface-raised/50 hover:bg-surface-raised/30 transition-colors">
                  <td className="p-4 text-ink font-medium">{c.name}</td>
                  <td className="p-4 text-ink-soft">{c.description || '—'}</td>
                  <td className="p-4 text-center">
                    <span
                      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${
                        c.active
                          ? 'bg-success/10 text-success border border-success/20'
                          : 'bg-tertiary/10 text-tertiary border border-tertiary/20'
                      }`}
                    >
                      <span
                        className={`w-1.5 h-1.5 rounded-full ${c.active ? 'bg-success' : 'bg-tertiary'}`}
                        aria-hidden="true"
                      />
                      {c.active ? 'Activa' : 'Inactiva'}
                    </span>
                  </td>
                  <td className="p-4 text-right">
                    <button
                      onClick={() => handleOpenEdit(c)}
                      className="text-accent hover:text-primary-soft transition-colors"
                      title="Editar"
                    >
                      Editar
                    </button>
                    <button
                      onClick={() => handleToggleActive(c)}
                      className="ml-3 text-ink-faint hover:text-danger transition-colors"
                      title={c.active ? 'Desactivar' : 'Activar'}
                    >
                      {c.active ? 'Desactivar' : 'Activar'}
                    </button>
                  </td>
                </tr>
              ))}
              {categories.length === 0 && (
                <tr>
                  <td colSpan={4} className="p-8 text-center text-ink-faint">
                    No hay categorías registradas. Crea la primera haciendo clic en "Nueva categoría".
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
                  {editing ? 'Editar categoría' : 'Nueva categoría'}
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
                    Nombre de la categoría *
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

                <div className="flex items-center gap-3">
                  <input
                    id="active"
                    type="checkbox"
                    checked={form.active ?? true}
                    onChange={(e) => handleInputChange('active', e.target.checked)}
                    className="rounded border-surface-raised text-primary focus:ring-accent"
                  />
                  <label htmlFor="active" className="text-sm font-medium text-ink-soft">
                    Categoría activa
                  </label>
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
                    {editing ? 'Guardar cambios' : 'Crear categoría'}
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

