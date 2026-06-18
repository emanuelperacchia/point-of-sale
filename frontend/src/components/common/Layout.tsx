import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

interface LayoutProps {
  children: React.ReactNode;
  title?: string;
}

const navItems = [
  { to: '/pos', label: 'POS' },
  { to: '/dashboard', label: 'Dashboard' },
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
  const { user, logout } = useAuth();

  return (
    <div className="flex h-screen bg-gray-100">
      {/* Sidebar */}
      <aside className="flex w-56 flex-col bg-gray-900 text-white">
        <div className="px-4 py-5">
          <h1 className="text-lg font-bold">POS System</h1>
        </div>
        <nav className="flex-1 space-y-1 px-3">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `block rounded px-3 py-2 text-sm transition-colors ${
                  isActive
                    ? 'bg-gray-700 text-white font-medium'
                    : 'text-gray-300 hover:bg-gray-800 hover:text-white'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-gray-700 px-4 py-3">
          <div className="text-sm text-gray-400 truncate">{user?.fullName}</div>
          <button
            onClick={logout}
            className="mt-2 w-full rounded bg-gray-700 px-3 py-1 text-xs text-gray-300 hover:bg-gray-600"
          >
            Cerrar sesion
          </button>
        </div>
      </aside>

      {/* Main content */}
      <div className="flex flex-1 flex-col">
        {title && (
          <header className="bg-white px-6 py-3 shadow-sm">
            <h2 className="text-lg font-semibold text-gray-800">{title}</h2>
          </header>
        )}
        <main className="flex-1 overflow-auto p-6">{children}</main>
      </div>
    </div>
  );
}
