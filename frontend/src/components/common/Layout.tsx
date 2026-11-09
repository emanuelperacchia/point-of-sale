import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

interface LayoutProps {
  children: React.ReactNode;
  title?: string;
}

const navItems = [
  { to: '/pos', label: 'POS' },
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/reports/consolidated', label: 'Corporativo' },
  { to: '/reports/sales-advanced', label: 'Reportes Ventas' },
  { to: '/analysis/products', label: 'Análisis ABC' },
  { to: '/analysis/inventory', label: 'Inventario' },
  { to: '/analysis/profitability', label: 'Rentabilidad' },
  { to: '/hr/report', label: 'RRHH' },
  { to: '/commissions', label: 'Comisiones' },
  { to: '/payroll', label: 'Nomina' },
  { to: '/recipes', label: 'Recetas' },
  { to: '/production-orders', label: 'Produccion' },
];

export default function Layout({ children, title }: LayoutProps) {
  const { user, logout, branchId, branches, switchBranch } = useAuth();

  return (
    <div className="flex h-screen bg-background">
      {/* Sidebar */}
      <aside className="flex w-56 flex-col bg-primary-dark text-ink-on-dark">
        <div className="flex items-center gap-2 px-4 py-5">
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-primary">
            <svg viewBox="0 0 48 48" className="h-5 w-5" aria-hidden="true">
              <rect width="48" height="48" rx="10" fill="#1C2541" />
              <text
                x="24" y="33" textAnchor="middle"
                fontFamily="Segoe UI, system-ui, sans-serif"
                fontSize="26" fontWeight="600" fill="#C9A24B"
              >N</text>
            </svg>
          </div>
          <span className="text-sm font-semibold uppercase tracking-[0.25em] text-ink-on-dark">
            Nivex
          </span>
        </div>
        <nav className="flex-1 space-y-1 px-3">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `block rounded-md px-3 py-2 text-sm transition-colors ${
                  isActive
                    ? 'bg-primary text-accent-soft font-medium'
                    : 'text-ink-on-dark/70 hover:bg-primary-soft hover:text-ink-on-dark'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-primary-soft/40 px-4 py-3">
          {/* Branch Selector */}
          {branches.length > 1 && (
            <div className="mb-2">
              <label className="block text-[10px] font-medium uppercase tracking-wider text-ink-faint">
                Sucursal
              </label>
              <select
                value={branchId ?? ''}
                onChange={(e) => {
                  const id = Number(e.target.value);
                  if (id && id !== branchId) switchBranch(id);
                }}
                className="mt-1 w-full rounded-md border border-primary-soft/40 bg-primary px-2 py-1 text-xs text-ink-on-dark focus:outline-none focus:ring-1 focus:ring-accent"
              >
                {branchId == null && <option value="">Todas</option>}
                {branches.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.nombre}
                  </option>
                ))}
              </select>
            </div>
          )}
          <div className="truncate text-sm text-ink-on-dark/80">{user?.fullName}</div>
          <div className="mt-2 space-y-1">
            <NavLink
              to="/mi-perfil"
              className="block w-full rounded-md bg-primary px-3 py-1 text-center text-xs text-ink-on-dark/80 hover:bg-primary-soft hover:text-ink-on-dark"
            >
              Mi perfil
            </NavLink>
            <button
              onClick={logout}
              className="w-full rounded-md bg-primary px-3 py-1 text-xs text-ink-on-dark/80 hover:bg-primary-soft hover:text-ink-on-dark"
            >
              Cerrar sesion
            </button>
          </div>
        </div>
      </aside>

      {/* Main content */}
      <div className="flex flex-1 flex-col">
        {title && (
          <header className="bg-surface px-6 py-3 shadow-soft">
            <h2 className="text-lg font-semibold text-ink">{title}</h2>
          </header>
        )}
        <main className="flex-1 overflow-auto p-6">{children}</main>
      </div>
    </div>
  );
}