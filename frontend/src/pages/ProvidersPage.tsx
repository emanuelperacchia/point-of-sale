import { useState, useEffect } from 'react';
import Layout from '../components/common/Layout';
import { supplierApi } from '../services/api';
import type { SupplierResponse, SupplierRequest, PaymentTerm, PAYMENT_TERM_LABELS } from '../types';

export default function ProvidersPage() {
  const [suppliers, setSuppliers] = useState<SupplierResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [openForm, setOpenForm] = useState(false);
  const [editing, setEditing] = useState(false);
  const [currentSupplier, setCurrentSupplier] = useState<SupplierResponse | null>(null);

  const [form, setForm] = useState<SupplierRequest>({
    code: '',
    taxId: '',
    businessName: '',
    tradeName: '',
    address: '',
    city: '',
    state: '',
    postalCode: '',
    country: 'Argentina',
    phone: '',
    email: '',
    website: '',
    categoryId: undefined,
    paymentTerm: 'NET_30',
    discountPercentage: 0,
    creditLimit: 0,
    notes: '',
    active: true,
  });

  useEffect(() => {
    loadSuppliers();
  }, []);

  const loadSuppliers = async () => {
    setLoading(true);
    setError('');
    try {
      const { data } = await supplierApi.findAll();
      setSuppliers(data);
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Error al cargar proveedores';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (key: keyof SupplierRequest, value: string | number | boolean) => {
    setForm({ ...form, [key]: value });
  };

  const handleOpenCreate = () => {
    setEditing(false);
    setCurrentSupplier(null);
    setForm({
      code: '',
      taxId: '',
      businessName: '',
      tradeName: '',
      address: '',
      city: '',
      state: '',
      postalCode: '',
      country: 'Argentina',
      phone: '',
      email: '',
      website: '',
      categoryId: undefined,
      paymentTerm: 'NET_30',
      discountPercentage: 0,
      creditLimit: 0,
      notes: '',
      active: true,
    });
    setOpenForm(true);
  };

  const handleOpenEdit = (supplier: SupplierResponse) => {
    setEditing(true);
    setCurrentSupplier(supplier);
    setForm({
      code: supplier.code,
      taxId: supplier.taxId,
      businessName: supplier.businessName,
      tradeName: supplier.tradeName ?? '',
      address: supplier.address ?? '',
      city: supplier.city ?? '',
      state: supplier.state ?? '',
      postalCode: supplier.postalCode ?? '',
      country: supplier.country ?? 'Argentina',
      phone: supplier.phone ?? '',
      email: supplier.email ?? '',
      website: supplier.website ?? '',
      categoryId: supplier.categoryId,
      paymentTerm: supplier.paymentTerm,
      discountPercentage: supplier.discountPercentage,
      creditLimit: supplier.creditLimit,
      notes: supplier.notes ?? '',
      active: supplier.active,
    });
    setOpenForm(true);
  };

  const handleSave = async () => {
    setError('');
    try {
      if (editing && currentSupplier) {
        await supplierApi.update(currentSupplier.id, form);
      } else {
        await supplierApi.create(form);
      }
      setOpenForm(false);
      setEditing(false);
      await loadSuppliers();
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Error al guardar proveedor';
      setError(msg);
    }
  };

  const handleToggleActive = async (supplier: SupplierResponse) => {
    setError('');
    try {
      await supplierApi.update(supplier.id, { ...supplier, active: !supplier.active });
      await loadSuppliers();
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Error al actualizar estado de proveedor';
      setError(msg);
    }
  };

  if (loading) {
    return (
      <Layout title="Proveedores">
        <div className="flex h-full items-center justify-center text-ink-faint">
          Cargando proveedores…
        </div>
      </Layout>
    );
  }

  return (
    <Layout title="Proveedores">
      <div className="space-y-6">
        {/* Encabezado */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-ink">Proveedores</h1>
            <p className="text-sm text-ink-faint">
              Gestiona los proveedores de tu red Nivex. Controla códigos, CUIT/RUT, plazos de pago y deuda.
            </p>
          </div>
          <button
            onClick={handleOpenCreate}
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2.5 text-sm font-medium text-ink-on-dark transition-colors hover:bg-primary-soft"
          >
            <span aria-hidden="true">+</span> Nuevo proveedor
          </button>
        </div>

        {error && (
          <div className="rounded-md border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger">
            {error}
          </div>
        )}

        {/* Tabla de proveedores */}
        <div className="rounded-xl border border-surface-raised bg-white/80 shadow-sm">
          <table className="w-full text-sm text-left border-collapse">
            <thead>
              <tr className="border-b border-surface-raised bg-surface-raised/50 text-ink-soft">
                <th className="p-4 font-medium text-ink">Código</th>
                <th className="p-4 font-medium text-ink">Razón Social</th>
                <th className="p-4 font-medium text-ink">CUIT / RUT</th>
                <th className="p-4 font-medium text-ink-soft">Teléfono</th>
                <th className="p-4 font-medium text-ink-soft text-center">Plazo Pago</th>
                <th className="p-4 font-medium text-ink-soft text-right">Estado</th>
                <th className="p-4 font-medium text-ink-soft text-right">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {suppliers.map((s) => (
                <tr key={s.id} className="border-b border-surface-raised/50 hover:bg-surface-raised/30 transition-colors">
                  <td className="p-4 text-ink font-medium">{s.code}</td>
                  <td className="p-4 text-ink">{s.businessName}</td>
                  <td className="p-4 text-ink-soft">{s.taxId}</td>
                  <td className="p-4 text-ink-soft">{s.phone || '—'}</td>
                  <td className="p-4 text-center">
                    <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium bg-accent/10 text-accent border border-accent/20">
                      {PAYMENT_TERM_LABELS[s.paymentTerm]}
                    </span>
                  </td>
                  <td className="p-4 text-center">
                    <span
                      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${
                        s.active
                          ? 'bg-success/10 text-success border border-success/20'
                          : 'bg-tertiary/10 text-tertiary border border-tertiary/20'
                      }`}
                    >
                      <span
                        className={`w-1.5 h-1.5 rounded-full ${s.active ? 'bg-success' : 'bg-tertiary'}`}
                        aria-hidden="true"
                      />
                      {s.active ? 'Activo' : 'Inactivo'}
                    </span>
                  </td>
                  <td className="p-4 text-right">
                    <button
                      onClick={() => handleOpenEdit(s)}
                      className="text-accent hover:text-primary-soft transition-colors"
                      title="Editar"
                    >
                      Editar
                    </button>
                    <button
                      onClick={() => handleToggleActive(s)}
                      className="ml-3 text-ink-faint hover:text-danger transition-colors"
                      title={s.active ? 'Desactivar' : 'Activar'}
                    >
                      {s.active ? 'Desactivar' : 'Activar'}
                    </button>
                  </td>
                </tr>
              ))}
              {suppliers.length === 0 && (
                <tr>
                  <td colSpan={7} className="p-8 text-center text-ink-faint">
                    No hay proveedores registrados. Crea el primero haciendo clic en "Nuevo proveedor".
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
            <div className="relative w-full max-w-2xl rounded-xl bg-white p-6 shadow-lg border border-surface-raised">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-bold text-ink">
                  {editing ? 'Editar proveedor' : 'Nuevo proveedor'}
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
                      Código *
                    </label>
                    <input
                      type="text"
                      value={form.code}
                      onChange={(e) => handleInputChange('code', e.target.value)}
                      required
                      className="w-full rounded-md border border-surface-raised bg-white px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                    />
                  </div>

                  <div>
                    <label className="mb-1 block text-sm font-medium text-ink-soft">
                      CUIT / RUT *
                    </label>
                    <input
                      type="text"
                      value={form.taxId}
                      onChange={(e) => handleInputChange('taxId', e.target.value)}
                      required
                      className="w-full rounded-md border border-surface-raised bg-white px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                    />
                  </div>

                  <div className="md:col-span-2">
                    <label className="mb-1 block text-sm font-medium text-ink-soft">
                      Razón Social *
                    </label>
                    <input
                      type="text"
                      value={form.businessName}
                      onChange={(e) => handleInputChange('businessName', e.target.value)}
                      required
                      className="w-full rounded-md border border-surface-raised bg-white px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                    />
                  </div>

                  <div>
                    <label className="mb-1 block text-sm font-medium text-ink-soft">
                      Nombre Comercial
                    </label>
                    <input
                      type="text"
                      value={form.tradeName}
                      onChange={(e) => handleInputChange('tradeName', e.target.value)}
                      className="w-full rounded-md border border-surface-raised bg-white px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                    />
                  </div>

                  <div className="md:col-span-2">
                    <label className="mb-1 block text-sm font-medium text-ink-soft">
                      Dirección
                    </label>
                    <input
                      type="text"
                      value={form.address}
                      onChange={(e) => handleInputChange('address', e.target.value)}
                      className="w-full rounded-md border border-surface-raised bg-white px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                    />
                  </div>

                  <div className="grid grid-cols-4 gap-4">
                    <div>
                      <label className="mb-1 block text-sm font-medium text-ink-soft">
                        Ciudad
                      </label>
                      <input
                        type="text"
                        value={form.city}
                        onChange={(e) => handleInputChange('city', e.target.value)}
                        className="w-full rounded-md border border-surface-raised bg-white px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                      />
                    </div>
                    <div>
                      <label className="mb-1 block text-sm font-medium text-ink-soft">
                        Provincia
                      </label>
                      <input
                        type="text"
                        value={form.state}
                        onChange={(e) => handleInputChange('state', e.target.value)}
                        className="w-full rounded-md border border-surface-raised bg-white px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                      />
                    </div>
                    <div>
                      <label className="mb-1 block text-sm font-medium text-ink-soft">
                        Código Postal
                      </label>
                      <input
                        type="text"
                        value={form.postalCode}
                        onChange={(e) => handleInputChange('postalCode', e.target.value)}
                        className="w-full rounded-md border border-surface-raised bg-white px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                      />
                    </div>
                    <div>
                      <label className="mb-1 block text-sm font-medium text-ink-soft">
                        País
                      </label>
                      <input
                        type="text"
                        value={form.country}
                        onChange={(e) => handleInputChange('country', e.target.value)}
                        className="w-full rounded-md border border-surface-raised bg-white px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-3 gap-4">
                    <div>
                      <label className="mb-1 block text-sm font-medium text-ink-soft">
                        Teléfono
                      </label>
                      <input
                        type="tel"
                        value={form.phone}
                        onChange={(e) => handleInputChange('phone', e.target.value)}
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
                    <div>
                      <label className="mb-1 block text-sm font-medium text-ink-soft">
                        Web
                      </label>
                      <input
                        type="text"
                        value={form.website}
                        onChange={(e) => handleInputChange('website', e.target.value)}
                        className="w-full rounded-md border border-surface-raised bg-white px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-3 gap-4">
                    <div>
                      <label className="mb-1 block text-sm font-medium text-ink-soft">
                        Plazo de Pago
                      </label>
                      <select
                        value={form.paymentTerm}
                        onChange={(e) => handleInputChange('paymentTerm', e.target.value as PaymentTerm)}
                        className="w-full rounded-md border border-surface-raised bg-white px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                      >
                        {Object.entries(PAYMENT_TERM_LABELS).map(([key, label]) => (
                          <option key={key} value={key as PaymentTerm}>
                            {label}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div>
                      <label className="mb-1 block text-sm font-medium text-ink-soft">
                        Descuento %
                      </label>
                      <input
                        type="number"
                        value={form.discountPercentage ?? 0}
                        onChange={(e) => handleInputChange('discountPercentage', Number(e.target.value) || 0)}
                        className="w-full rounded-md border border-surface-raised bg-white px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                      />
                    </div>
                    <div>
                      <label className="mb-1 block text-sm font-medium text-ink-soft">
                        Límite Crédito
                      </label>
                      <input
                        type="number"
                        value={form.creditLimit ?? 0}
                        onChange={(e) => handleInputChange('creditLimit', Number(e.target.value) || 0)}
                        className="w-full rounded-md border border-surface-raised bg-white px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                      />
                    </div>
                  </div>

                  <div className="md:col-span-2">
                    <label className="mb-1 block text-sm font-medium text-ink-soft">
                      Notas
                    </label>
                    <textarea
                      value={form.notes}
                      onChange={(e) => handleInputChange('notes', e.target.value)}
                      rows={2}
                      className="w-full rounded-md border border-surface-raised bg-white px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                    />
                  </div>

                  <div>
                    <div className="flex items-center gap-3">
                      <input
                        id="active"
                        type="checkbox"
                        checked={form.active ?? true}
                        onChange={(e) => handleInputChange('active', e.target.checked)}
                        className="rounded border-surface-raised text-primary focus:ring-accent"
                      />
                      <label htmlFor="active" className="text-sm font-medium text-ink-soft">
                        Proveedor activo
                      </label>
                    </div>
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
                    {editing ? 'Guardar cambios' : 'Crear proveedor'}
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
