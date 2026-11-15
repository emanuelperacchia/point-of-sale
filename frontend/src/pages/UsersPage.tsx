import { useState, useEffect } from 'react';
import Layout from '../components/common/Layout';
import { userApi } from '../services/api';
import type { UserResponse } from '../types';

export default function UsersPage() {
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [openForm, setOpenForm] = useState(false);
  const [viewing, setViewing] = useState(false);
  const [currentUser, setCurrentUser] = useState<UserResponse | null>(null);
  const [userPermissions, setUserPermissions] = useState<string[]>([]);

  useEffect(() => {
    loadUsers();
  }, []);

  const loadUsers = async () => {
    setLoading(true);
    setError('');
    try {
      const { data } = await userApi.getAllUsers();
      setUsers(data);
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Error al cargar usuarios';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleViewUser = (user: UserResponse) => {
    setViewing(true);
    setCurrentUser(user);
    setOpenForm(true);
  };

  const handleToggleActive = async (user: UserResponse) => {
    setError('');
    try {
      // Usamos update vía userApi.getUserById + patch, pero como no hay endpoint de update en UserController,
      // usamos el endpoint de toggle vía el id. Como el backend no expone update de usuario en este controller,
      // solo mostramos la acción de desactivar como informativa o la dejamos para futuras mejoras.
      setError('La actualización de usuarios se gestionará en la próxima etapa');
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Error al actualizar usuario';
      setError(msg);
    }
  };

  const loadMyPermissions = async () => {
    try {
      const { data } = await userApi.getMyPermissions();
      setUserPermissions(data);
    } catch {
      // Ignorar error de permisos
    }
  };

  useEffect(() => {
    loadMyPermissions();
  }, []);

  if (loading) {
    return (
      <Layout title="Usuarios">
        <div className="flex h-full items-center justify-center text-ink-faint">
          Cargando usuarios…
        </div>
      </Layout>
    );
  }

  return (
    <Layout title="Usuarios">
      <div className="space-y-6">
        {/* Encabezado */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-ink">Usuarios</h1>
            <p className="text-sm text-ink-faint">
              Gestiona los usuarios del sistema de tu red Nivex. Roles, estado y permisos.
            </p>
          </div>
        </div>

        {error && (
          <div className="rounded-md border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger">
            {error}
          </div>
        )}

        {/* Mis permisos (informativo) */}
        <div className="rounded-xl border border-surface-raised bg-surface/80  p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-bold text-ink">Mis Permisos</h2>
            <p className="text-sm text-ink-faint">
              Permisos del usuario autenticado según el token JWT.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            {userPermissions.length > 0
              ? userPermissions.map((perm) => (
                  <span key={perm} className="px-2.5 py-1 rounded-md text-xs font-medium bg-accent/10 text-accent border border-accent/20">
                    {perm}
                  </span>
                ))
              : <span className="text-xs text-ink-faint">Sin permisos asignados</span>}
          </div>
        </div>

        {/* Tabla de usuarios */}
        <div className="rounded-xl border border-surface-raised bg-surface/80 ">
          <div className="p-4 border-b border-surface-raised bg-surface-raised/50">
            <h2 className="text-lg font-bold text-ink">Listado de Usuarios</h2>
          </div>

          <table className="w-full text-sm text-left border-collapse">
            <thead>
              <tr className="border-b border-surface-raised bg-surface-raised/50 text-ink-soft">
                <th className="p-4 font-medium text-ink">Nombre</th>
                <th className="p-4 font-medium text-ink">Email</th>
                <th className="p-4 font-medium text-ink-soft">Teléfono</th>
                <th className="p-4 font-medium text-ink-soft text-center">Roles</th>
                <th className="p-4 font-medium text-ink-soft text-center">Estado</th>
                <th className="p-4 font-medium text-ink-soft text-center">Último Login</th>
                <th className="p-4 font-medium text-ink-soft text-right">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id} className="border-b border-surface-raised/50 hover:bg-surface-raised/30 transition-colors">
                  <td className="p-4 text-ink font-medium">{u.fullName}</td>
                  <td className="p-4 text-ink-soft">{u.email}</td>
                  <td className="p-4 text-ink-soft">{u.phone || '—'}</td>
                  <td className="p-4 text-center">
                    <div className="flex items-center justify-center gap-1">
                      {u.roles.map((role) => (
                        <span key={role} className="px-2 py-0.5 rounded text-[10px] font-medium bg-surface-raised text-ink-soft border border-surface-raised/50">
                          {role}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td className="p-4 text-center">
                    <span
                      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${
                        u.active
                          ? 'bg-success/10 text-success border border-success/20'
                          : 'bg-tertiary/10 text-tertiary border border-tertiary/20'
                      }`}
                    >
                      <span
                        className={`w-1.5 h-1.5 rounded-full ${u.active ? 'bg-success' : 'bg-tertiary'}`}
                        aria-hidden="true"
                      />
                      {u.active ? 'Activo' : 'Inactivo'}
                    </span>
                  </td>
                  <td className="p-4 text-center text-ink-soft">
                    {u.lastLogin ? new Date(u.lastLogin).toLocaleDateString('es-AR') : '—'}
                  </td>
                  <td className="p-4 text-right">
                    <button
                      onClick={() => handleViewUser(u)}
                      className="text-accent hover:text-primary-soft transition-colors"
                      title="Ver detalles"
                    >
                      Ver
                    </button>
                  </td>
                </tr>
              ))}
              {users.length === 0 && (
                <tr>
                  <td colSpan={7} className="p-8 text-center text-ink-faint">
                    No hay usuarios registrados.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Modal detalles de usuario */}
        {openForm && currentUser && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <div
              className="absolute inset-0 bg-black/40 backdrop-blur-sm"
              onClick={() => setOpenForm(false)}
              aria-hidden="true"
            />
            <div className="relative w-full max-w-md rounded-xl bg-surface p-6  border border-surface-raised">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-bold text-ink">Detalles de Usuario</h2>
                <button
                  onClick={() => setOpenForm(false)}
                  className="rounded-md p-2 text-ink-faint hover:text-ink hover:bg-surface-raised transition-colors"
                  aria-label="Cerrar"
                >
                  <span aria-hidden="true">✕</span>
                </button>
              </div>

              <div className="space-y-4">
                <div>
                  <label className="mb-1 block text-sm font-medium text-ink-soft">
                    Nombre Completo
                  </label>
                  <p className="text-ink">{currentUser.fullName}</p>
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-ink-soft">
                    Email
                  </label>
                  <p className="text-ink">{currentUser.email}</p>
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-ink-soft">
                    Teléfono
                  </label>
                  <p className="text-ink">{currentUser.phone || 'No registrado'}</p>
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-ink-soft">
                    Roles
                  </label>
                  <div className="flex flex-wrap gap-2">
                    {currentUser.roles.map((role) => (
                      <span key={role} className="px-2.5 py-1 rounded-md text-xs font-medium bg-accent/10 text-accent border border-accent/20">
                        {role}
                      </span>
                    ))}
                  </div>
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-ink-soft">
                    Estado
                  </label>
                  <p
                    className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${
                      currentUser.active
                        ? 'bg-success/10 text-success border border-success/20'
                        : 'bg-tertiary/10 text-tertiary border border-tertiary/20'
                    }`}
                  >
                    <span
                      className={`w-1.5 h-1.5 rounded-full ${currentUser.active ? 'bg-success' : 'bg-tertiary'}`}
                      aria-hidden="true"
                    />
                    {currentUser.active ? 'Activo' : 'Inactivo'}
                  </p>
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-ink-soft">
                    Creado el
                  </label>
                  <p className="text-ink">{new Date(currentUser.createdAt).toLocaleDateString('es-AR')}</p>
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-ink-soft">
                    Último Login
                  </label>
                  <p className="text-ink">
                    {currentUser.lastLogin ? new Date(currentUser.lastLogin).toLocaleDateString('es-AR') : 'Nunca'}
                  </p>
                </div>
              </div>

              <div className="flex justify-end pt-4">
                <button
                  onClick={() => setOpenForm(false)}
                  className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-ink-on-dark transition-colors hover:bg-primary-soft"
                >
                  Cerrar
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
}

