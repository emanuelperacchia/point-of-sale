import { useAuth } from '../../context/AuthContext';

interface LayoutProps {
  children: React.ReactNode;
  title?: string;
}

export default function Layout({ children, title }: LayoutProps) {
  const { user, logout } = useAuth();

  return (
    <div className="flex h-screen flex-col bg-gray-100">
      {/* Barra superior */}
      <header className="flex items-center justify-between bg-white px-6 py-3 shadow-sm" role="banner">
        <div className="flex items-center gap-3">
          <h1 className="text-xl font-bold text-gray-800">POS System</h1>
          {title && <span className="text-sm text-gray-400">|</span>}
          {title && <span className="text-sm text-gray-600">{title}</span>}
        </div>
        <div className="flex items-center gap-4">
          <span className="text-sm text-gray-600">{user?.fullName}</span>
          <button
            onClick={logout}
            aria-label="Cerrar sesión"
            className="rounded bg-gray-200 px-3 py-1 text-sm text-gray-700 hover:bg-gray-300"
          >
            Salir
          </button>
        </div>
      </header>

      {/* Contenido */}
      <main className="flex-1 overflow-auto p-6">{children}</main>
    </div>
  );
}
