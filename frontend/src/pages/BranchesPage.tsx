import { useState, useEffect } from 'react';
import Layout from '../components/common/Layout';
import { branchApi } from '../services/api';
import type { BranchResponse, BranchRequest } from '../types';

export default function BranchesPage() {
  const [branches, setBranches] = useState<BranchResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [openForm, setOpenForm] = useState(false);
  const [editing, setEditing] = useState(false);
  const [currentBranch, setCurrentBranch] = useState<BranchResponse | null>(null);

  const [form, setForm] = useState<BranchRequest>({
    nombre: '',
    direccion: '',
    telefono: '',
    email: '',
    activa: true,
  });

  useEffect(() => {
    loadBranches();
  }, []);

  const loadBranches = async () => {
    setLoading(true);
    setError('');
    try {
      const { data } = await branchApi.listActive();
      setBranches(data);
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Error al cargar sucursales';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (key: keyof BranchRequest, value: string | boolean) => {
    setForm({ ...form, [key]: value });
  };

  const handleOpenCreate = () => {
    setEditing(false);
    setCurrentBranch(null);
    setForm({ nombre: '', direccion: '', telefono: '', email: '', activa: true });
    setOpenForm(true);
  };

  const handleOpenEdit = (branch: BranchResponse) => {
    setEditing(true);
    setCurrentBranch(branch);
    setForm({
      nombre: branch.nombre,
      direccion: branch.direccion ?? '',
      telefono: branch.telefono ?? '',
      email: branch.email ?? '',
      activa: branch.activa,
    });
    setOpenForm(true);
  };

  const handleSave = async () => {
    setError('');
    try {
      if (editing && currentBranch) {
        await branchApi.update(currentBranch.id, form);
      } else {
        await branchApi.create(form);
      }
      setOpenForm(false);
      setEditing(false);
      await loadBranches();
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Error al guardar sucursal';
      setError(msg);
    }
  };

  const handleToggleActive = async (branch: BranchResponse) => {
    setError('');
    try {
      await branchApi.update(branch.id, { ...branch, activa: !branch.activa });
      await loadBranches();
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Error al actualizar estado de sucursal';
      setError(msg);
    }
  };

  if (loading) {
    return (
      <Layout title="Sucursales">
        <div className="flex h-full items-center justify-center text-ink-faint">
          Cargando sucursales…
        </div>
      </Layout>
    );
  }

  return (
    <Layout title="Sucursales">
      <div className="space-y-6">
        {/* Encabezado */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-ink">Sucursales</h1>
            <p className="text-sm text-ink-faint">
              Gestiona los negocios de tu red Nivex. Cada sucursal tiene su propio punto de venta fiscal y precios.
            </p>
          </div>
          <button
            onClick={handleOpenCreate}
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2.5 text-sm font-medium text-ink-on-dark transition-colors hover:bg-primary-soft"
          >
            <span aria-hidden="true">+</span> Nueva sucursal
          </button>
        </div>

        {error && (
          <div className="rounded-md border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger">
            {error}
          </div>
        )}

        {/* Tabla de sucursales */}
        <div className="rounded-xl border border-surface-raised bg-white/80 shadow-sm">
          <table className="w-full text-sm text-left border-collapse">
            <thead>
              <tr className="border-b border-surface-raised bg-surface-raised/50 text-ink-soft">
                <th className="p-4 font-medium text-ink">Nombre</th>
                <th className="p-4 font-medium text-ink-soft">Dirección</th>
                <th className="p-4 font-medium text-ink-soft">Teléfono</th>
                <th className="p-4 font-medium text-ink-soft">Email</th>
                <th className="p-4 font-medium text-ink-soft text-center">Estado</th>
                <th className="p-4 font-medium text-ink-soft text-right">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {branches.map((b) => (
                <tr key={b.id} className="border-b border-surface-raised/50 hover:bg-surface-raised/30 transition-colors">
                  <td className="p-4 text-ink font-medium">{b.nombre}</td>
                  <td className="p-4 text-ink-soft">{b.direccion || '—'}</td>
                  <td className="p-4 text-ink-soft">{b.telefono || '—'}</td>
                  <td className="p-4 text-ink-soft">{b.email || '—'}</td>
                  <td className="p-4 text-center">
                    <span
                      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${
                        b.activa
                          ? 'bg-success/10 text-success border border-success/20'
                          : 'bg-tertiary/10 text-tertiary border border-tertiary/20'
                      }`}
                    >
                      <span
                        className={`w-1.5 h-1.5 rounded-full ${
                          b.activa ? 'bg-success' : 'bg-tertiary'
                        }`}
                        aria-hidden="true"
                      />
                      {b.activa ? 'Activa' : 'Inactiva'}
                    </span>
                  </td>
                  <td className="p-4 text-right">
                    <button
                      onClick={() => handleOpenEdit(b)}
                      className="text-accent hover:text-primary-soft transition-colors"
                      title="Editar"
                    >
                      Editar
                    </button>
                    <button
                      onClick={() => handleToggleActive(b)}
                      className="ml-3 text-ink-faint hover:text-danger transition-colors"
                      title={b.activa ? 'Desactivar' : 'Activar'}
                    >
                      {b.activa ? 'Desactivar' : 'Activar'}
                    </button>
                  </td>
                </tr>
              ))}
              {branches.length === 0 && (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-ink-faint">
                    No hay sucursales registradas. Crea la primera haciendo clic en "Nueva sucursal".
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
            <div className="relative w-full max-w-md rounded-xl bg-white p-6 shadow-lg border border-surface-raised">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-bold text-ink">
                  {editing ? 'Editar sucursal' : 'Nueva sucursal'}
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
                    Nombre de la sucursal *
                  </label>
                  <input
                    type="text"
                    value={form.nombre}
                    onChange={(e) => handleInputChange('nombre', e.target.value)}
                    required
                    className="w-full rounded-md border border-surface-raised bg-white px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                  />
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-ink-soft">
                    Dirección
                  </label>
                  <input
                    type="text"
                    value={form.direccion}
                    onChange={(e) => handleInputChange('direccion', e.target.value)}
                    className="w-full rounded-md border border-surface-raised bg-white px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="mb-1 block text-sm font-medium text-ink-soft">
                      Teléfono
                    </label>
                    <input
                      type="tel"
                      value={form.telefono}
                      onChange={(e) => handleInputChange('telefono', e.target.value)}
                      className="w-full rounded-md border border-surface-raised bg-white px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-sm font-medium text-ink-soft">
                      Email
                    </label>
                    <input
                      type="email"
                      value={form.email}
                      onChange={(e) => handleInputChange('email', e.target.value)}
                      className="w-full rounded-md border border-surface-raised bg-white px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                    />
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <input
                    id="activa"
                    type="checkbox"
                    checked={form.activa ?? true}
                    onChange={(e) => handleInputChange('activa', e.target.checked)}
                    className="rounded border-surface-raised text-primary focus:ring-accent"
                  />
                  <label htmlFor="activa" className="text-sm font-medium text-ink-soft">
                    Sucursal activa
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
                    className="rounded-md px-4 py-2 text-sm font-medium text-ink-soft bg-white border border-surface-raised hover:bg-surface-raised transition-colors"
                  >
                    Cancelar
                  </button>
                  <button
                    type="submit"
                    className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-ink-on-dark transition-colors hover:bg-primary-soft"
                  >
                    {editing ? 'Guardar cambios' : 'Crear sucursal'}
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
