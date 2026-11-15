import { useState, useEffect } from 'react';
import Layout from '../components/common/Layout';
import { employeeApi } from '../services/api';
import type { EmployeeResponse, EmployeeRequest, ModalidadContrato } from '../types';

const MODALIDAD_LABELS: Record<ModalidadContrato, string> = {
  FULL_TIME: 'Tiempo completo',
  PART_TIME: 'Medio tiempo',
  CONTRATO: 'Contrato',
};

export default function EmployeesPage() {
  const [employees, setEmployees] = useState<EmployeeResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [openForm, setOpenForm] = useState(false);
  const [editing, setEditing] = useState(false);
  const [currentEmployee, setCurrentEmployee] = useState<EmployeeResponse | null>(null);

  const [form, setForm] = useState<EmployeeRequest>({
    nombre: '',
    apellido: '',
    dni: '',
    cuil: '',
    fechaNacimiento: '',
    fechaIngreso: '',
    cargo: '',
    departamento: '',
    sucursalId: undefined,
    salarioBase: 0,
    modalidadContrato: 'FULL_TIME',
    userId: undefined,
  });

  useEffect(() => {
    loadEmployees();
  }, []);

  const loadEmployees = async () => {
    setLoading(true);
    setError('');
    try {
      const { data } = await employeeApi.getAll();
      setEmployees(data);
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Error al cargar empleados';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (key: keyof EmployeeRequest, value: string | number | boolean) => {
    setForm({ ...form, [key]: value });
  };

  const handleOpenCreate = () => {
    setEditing(false);
    setCurrentEmployee(null);
    setForm({
      nombre: '',
      apellido: '',
      dni: '',
      cuil: '',
      fechaNacimiento: '',
      fechaIngreso: '',
      cargo: '',
      departamento: '',
      sucursalId: undefined,
      salarioBase: 0,
      modalidadContrato: 'FULL_TIME',
      userId: undefined,
    });
    setOpenForm(true);
  };

  const handleOpenEdit = (employee: EmployeeResponse) => {
    setEditing(true);
    setCurrentEmployee(employee);
    setForm({
      nombre: employee.nombre,
      apellido: employee.apellido,
      dni: employee.dni,
      cuil: employee.cuil ?? '',
      fechaNacimiento: employee.fechaNacimiento,
      fechaIngreso: employee.fechaIngreso,
      cargo: employee.cargo,
      departamento: employee.departamento,
      sucursalId: employee.sucursalId,
      salarioBase: employee.salarioBase,
      modalidadContrato: employee.modalidadContrato,
      userId: employee.userId,
    });
    setOpenForm(true);
  };

  const handleSave = async () => {
    setError('');
    try {
      if (editing && currentEmployee) {
        await employeeApi.update(currentEmployee.id, form);
      } else {
        await employeeApi.create(form);
      }
      setOpenForm(false);
      setEditing(false);
      await loadEmployees();
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Error al guardar empleado';
      setError(msg);
    }
  };

  const handleDeactivate = async (id: number) => {
    setError('');
    if (!window.confirm('¿Estás seguro de desactivar este empleado?')) return;
    try {
      await employeeApi.deactivate(id);
      await loadEmployees();
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Error al desactivar empleado';
      setError(msg);
    }
  };

  if (loading) {
    return (
      <Layout title="Empleados">
        <div className="flex h-full items-center justify-center text-ink-faint">
          Cargando empleados…
        </div>
      </Layout>
    );
  }

  return (
    <Layout title="Empleados">
      <div className="space-y-6">
        {/* Encabezado */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-ink">Empleados</h1>
            <p className="text-sm text-ink-faint">
              Gestiona el personal de tu red Nivex. Datos personales, cargo, departamento y salario.
            </p>
          </div>
          <button
            onClick={handleOpenCreate}
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2.5 text-sm font-medium text-ink-on-dark transition-colors hover:bg-primary-soft"
          >
            <span aria-hidden="true">+</span> Nuevo empleado
          </button>
        </div>

        {error && (
          <div className="rounded-md border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger">
            {error}
          </div>
        )}

        {/* Tabla de empleados */}
        <div className="rounded-xl border border-surface-raised bg-surface/80 ">
          <table className="w-full text-sm text-left border-collapse">
            <thead>
              <tr className="border-b border-surface-raised bg-surface-raised/50 text-ink-soft">
                <th className="p-4 font-medium text-ink">Nombre</th>
                <th className="p-4 font-medium text-ink-soft">DNI / CUIL</th>
                <th className="p-4 font-medium text-ink-soft">Cargo</th>
                <th className="p-4 font-medium text-ink-soft">Departamento</th>
                <th className="p-4 font-medium text-ink-soft text-right">Salario</th>
                <th className="p-4 font-medium text-ink-soft text-center">Modalidad</th>
                <th className="p-4 font-medium text-ink-soft text-center">Estado</th>
                <th className="p-4 font-medium text-ink-soft text-right">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {employees.map((e) => (
                <tr key={e.id} className="border-b border-surface-raised/50 hover:bg-surface-raised/30 transition-colors">
                  <td className="p-4 text-ink font-medium">{e.nombre} {e.apellido}</td>
                  <td className="p-4 text-ink-soft">{e.dni} {e.cuil ? '| ' + e.cuil : ''}</td>
                  <td className="p-4 text-ink-soft">{e.cargo}</td>
                  <td className="p-4 text-ink-soft">{e.departamento}</td>
                  <td className="p-4 text-right text-ink-soft font-mono">
                    ${e.salarioBase.toFixed(2)}
                  </td>
                  <td className="p-4 text-center">
                    <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium bg-accent/10 text-accent border border-accent/20">
                      {MODALIDAD_LABELS[e.modalidadContrato]}
                    </span>
                  </td>
                  <td className="p-4 text-center">
                    <span
                      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${
                        e.activo
                          ? 'bg-success/10 text-success border border-success/20'
                          : 'bg-tertiary/10 text-tertiary border border-tertiary/20'
                      }`}
                    >
                      <span
                        className={`w-1.5 h-1.5 rounded-full ${e.activo ? 'bg-success' : 'bg-tertiary'}`}
                        aria-hidden="true"
                      />
                      {e.activo ? 'Activo' : 'Inactivo'}
                    </span>
                  </td>
                  <td className="p-4 text-right">
                    <button
                      onClick={() => handleOpenEdit(e)}
                      className="text-accent hover:text-primary-soft transition-colors"
                      title="Editar"
                    >
                      Editar
                    </button>
                    <button
                      onClick={() => handleDeactivate(e.id)}
                      className="ml-3 text-ink-faint hover:text-danger transition-colors"
                      title={e.activo ? 'Desactivar' : 'Activar'}
                    >
                      {e.activo ? 'Desactivar' : 'Activar'}
                    </button>
                  </td>
                </tr>
              ))}
              {employees.length === 0 && (
                <tr>
                  <td colSpan={8} className="p-8 text-center text-ink-faint">
                    No hay empleados registrados. Crea el primero haciendo clic en "Nuevo empleado".
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
                  {editing ? 'Editar empleado' : 'Nuevo empleado'}
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
                      Nombre *
                    </label>
                    <input
                      type="text"
                      value={form.nombre}
                      onChange={(e) => handleInputChange('nombre', e.target.value)}
                      required
                      autoFocus
                      className="w-full rounded-md border border-surface-raised bg-surface px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-sm font-medium text-ink-soft">
                      Apellido *
                    </label>
                    <input
                      type="text"
                      value={form.apellido}
                      onChange={(e) => handleInputChange('apellido', e.target.value)}
                      required
                      className="w-full rounded-md border border-surface-raised bg-surface px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-sm font-medium text-ink-soft">
                      DNI *
                    </label>
                    <input
                      type="text"
                      value={form.dni}
                      onChange={(e) => handleInputChange('dni', e.target.value)}
                      required
                      className="w-full rounded-md border border-surface-raised bg-surface px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-sm font-medium text-ink-soft">
                      CUIL
                    </label>
                    <input
                      type="text"
                      value={form.cuil}
                      onChange={(e) => handleInputChange('cuil', e.target.value)}
                      className="w-full rounded-md border border-surface-raised bg-surface px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                    />
                  </div>
                  <div className="md:col-span-2">
                    <label className="mb-1 block text-sm font-medium text-ink-soft">
                      Cargo *
                    </label>
                    <input
                      type="text"
                      value={form.cargo}
                      onChange={(e) => handleInputChange('cargo', e.target.value)}
                      required
                      className="w-full rounded-md border border-surface-raised bg-surface px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-sm font-medium text-ink-soft">
                      Departamento *
                    </label>
                    <input
                      type="text"
                      value={form.departamento}
                      onChange={(e) => handleInputChange('departamento', e.target.value)}
                      required
                      className="w-full rounded-md border border-surface-raised bg-surface px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                    />
                  </div>
                  <div className="grid grid-cols-3 gap-4">
                    <div>
                      <label className="mb-1 block text-sm font-medium text-ink-soft">
                        Fecha Nacimiento
                      </label>
                      <input
                        type="date"
                        value={form.fechaNacimiento}
                        onChange={(e) => handleInputChange('fechaNacimiento', e.target.value)}
                        required
                        className="w-full rounded-md border border-surface-raised bg-surface px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                      />
                    </div>
                    <div>
                      <label className="mb-1 block text-sm font-medium text-ink-soft">
                        Fecha Ingreso
                      </label>
                      <input
                        type="date"
                        value={form.fechaIngreso}
                        onChange={(e) => handleInputChange('fechaIngreso', e.target.value)}
                        required
                        className="w-full rounded-md border border-surface-raised bg-surface px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                      />
                    </div>
                    <div>
                      <label className="mb-1 block text-sm font-medium text-ink-soft">
                        Salario Base
                      </label>
                      <input
                        type="number"
                        step="0.01"
                        value={form.salarioBase}
                        onChange={(e) => handleInputChange('salarioBase', Number(e.target.value) || 0)}
                        required
                        className="w-full rounded-md border border-surface-raised bg-surface px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                      />
                    </div>
                  </div>
                  <div>
                    <label className="mb-1 block text-sm font-medium text-ink-soft">
                      Modalidad
                    </label>
                    <select
                      value={form.modalidadContrato}
                      onChange={(e) => handleInputChange('modalidadContrato', e.target.value as ModalidadContrato)}
                      className="w-full rounded-md border border-surface-raised bg-surface px-3 py-2 text-ink focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
                    >
                      {Object.entries(MODALIDAD_LABELS).map(([key, label]) => (
                        <option key={key} value={key as ModalidadContrato}>
                          {label}
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
                    {editing ? 'Guardar cambios' : 'Crear empleado'}
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

