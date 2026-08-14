import { useState, type FormEvent } from 'react';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const { login, isLoading } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      await login(email, password);
      window.location.href = '/pos';
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'response' in err) {
        const axiosErr = err as { response?: { data?: { message?: string } } };
        setError(axiosErr.response?.data?.message || 'Credenciales inválidas');
      } else {
        setError('Error de conexión');
      }
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4">
      <div className="w-full max-w-sm">
        {/* Wordmark Nivex */}
        <div className="mb-8 flex flex-col items-center">
          <div className="flex h-14 w-14 items-center justify-center rounded-xl bg-primary shadow-soft">
            <svg viewBox="0 0 48 48" className="h-8 w-8" aria-hidden="true">
              <rect width="48" height="48" rx="10" fill="#1C2541" />
              <text
                x="24" y="33" textAnchor="middle"
                fontFamily="Segoe UI, system-ui, sans-serif"
                fontSize="26" fontWeight="600" fill="#C9A24B"
              >N</text>
              <circle cx="35" cy="11" r="2.2" fill="#C9A24B" opacity="0.85" />
            </svg>
          </div>
          <span className="mt-3 text-sm font-medium uppercase tracking-[0.35em] text-ink-soft">
            Nivex
          </span>
        </div>

        <div className="rounded-xl bg-white/70 p-8 shadow-lift backdrop-blur">
          <h1 className="mb-1 text-2xl font-semibold tracking-tight text-ink">
            Iniciar sesión
          </h1>
          <p className="mb-6 text-sm text-ink-faint">
            Punto de venta — acceso del personal
          </p>

          {error && (
            <div className="mb-4 rounded-md border border-danger/30 bg-danger/10 px-4 py-2 text-sm text-danger">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="mb-1 block text-sm font-medium text-ink-soft" htmlFor="email">
                Email
              </label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoFocus
                className="w-full rounded-md border border-surface-raised bg-white px-3 py-2 text-ink placeholder:text-ink-faint focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-ink-soft" htmlFor="password">
                Contraseña
              </label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                className="w-full rounded-md border border-surface-raised bg-white px-3 py-2 text-ink placeholder:text-ink-faint focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
              />
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full rounded-md bg-primary py-2.5 font-medium text-ink-on-dark transition-colors hover:bg-primary-soft disabled:opacity-50"
            >
              {isLoading ? 'Ingresando…' : 'Ingresar'}
            </button>
          </form>
        </div>

        <p className="mt-6 text-center text-xs text-ink-faint">
          © {new Date().getFullYear()} Nivex
        </p>
      </div>
    </div>
  );
}