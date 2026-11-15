import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { authApi } from '../services/api';
import type { RegisterRequest } from '../types';

export default function RegisterPage() {
  const { login } = useAuth();
  const [form, setForm] = useState<RegisterRequest>({
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    phone: '',
    termsAccepted: false,
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const handleInputChange = (key: keyof RegisterRequest, value: string | boolean) => {
    setForm({ ...form, [key]: value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess(false);

    if (!form.termsAccepted) {
      setError('Debes aceptar los Términos y Condiciones y la Política de Privacidad');
      return;
    }

    setIsLoading(true);
    try {
      const { data } = await authApi.register(form);
      // Después de registrar exitosamente, hacer login automático
      await login(form.email, form.password);
      setSuccess(true);
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Error al registrar usuario';
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  if (success) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background px-4">
        <div className="w-full max-w-md">
          <div className="rounded-xl bg-surface/70 p-8 shadow-lift backdrop-blur text-center">
            <div className="mb-4 text-5xl" aria-hidden="true">✅</div>
            <h1 className="mb-2 text-2xl font-bold text-ink">Registro exitoso</h1>
            <p className="mb-6 text-sm text-ink-faint">
              Tu cuenta ha sido creada. Serás redirigido al punto de venta.
            </p>
            <button
              onClick={() => {
                window.location.href = '/pos';
              }}
              className="rounded-md bg-primary px-6 py-2.5 text-sm font-medium text-ink-on-dark transition-colors hover:bg-primary-soft"
            >
              Ir al POS
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4">
      <div className="w-full max-w-md">
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

        <div className="rounded-xl bg-surface/70 p-8 shadow-lift backdrop-blur">
          <h1 className="mb-1 text-2xl font-semibold tracking-tight text-ink">
            Crear cuenta
          </h1>
          <p className="mb-6 text-sm text-ink-faint">
            Registro de personal — punto de venta
          </p>

          {error && (
            <div className="mb-4 rounded-md border border-danger/30 bg-danger/10 px-4 py-2 text-sm text-danger">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="mb-1 block text-sm font-medium text-ink-soft" htmlFor="firstName">
                Nombre *
              </label>
              <input
                id="firstName"
                type="text"
                value={form.firstName}
                onChange={(e) => handleInputChange('firstName', e.target.value)}
                required
                autoFocus
                className="w-full rounded-md border border-surface-raised bg-surface px-3 py-2 text-ink placeholder:text-ink-faint focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-ink-soft" htmlFor="lastName">
                Apellido *
              </label>
              <input
                id="lastName"
                type="text"
                value={form.lastName}
                onChange={(e) => handleInputChange('lastName', e.target.value)}
                required
                className="w-full rounded-md border border-surface-raised bg-surface px-3 py-2 text-ink placeholder:text-ink-faint focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-ink-soft" htmlFor="email">
                Email *
              </label>
              <input
                id="email"
                type="email"
                value={form.email}
                onChange={(e) => handleInputChange('email', e.target.value)}
                required
                className="w-full rounded-md border border-surface-raised bg-surface px-3 py-2 text-ink placeholder:text-ink-faint focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-ink-soft" htmlFor="password">
                Contraseña * (mínimo 8 caracteres)
              </label>
              <input
                id="password"
                type="password"
                value={form.password}
                onChange={(e) => handleInputChange('password', e.target.value)}
                required
                minLength="8"
                className="w-full rounded-md border border-surface-raised bg-surface px-3 py-2 text-ink placeholder:text-ink-faint focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-ink-soft" htmlFor="phone">
                Teléfono
              </label>
              <input
                id="phone"
                type="tel"
                value={form.phone}
                onChange={(e) => handleInputChange('phone', e.target.value)}
                className="w-full rounded-md border border-surface-raised bg-surface px-3 py-2 text-ink placeholder:text-ink-faint focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30"
              />
            </div>

            <div className="flex items-start gap-3">
              <input
                id="termsAccepted"
                type="checkbox"
                checked={form.termsAccepted}
                onChange={(e) => handleInputChange('termsAccepted', e.target.checked)}
                className="mt-0.5 rounded border-surface-raised text-primary focus:ring-accent"
              />
              <label
                htmlFor="termsAccepted"
                className="text-sm text-ink-soft leading-relaxed"
              >
                Acepto los{' '}
                <a href="/terminos" className="underline hover:text-accent">
                  Términos y Condiciones
                </a>{' '}
                y la{' '}
                <a href="/privacidad" className="underline hover:text-accent">
                  Política de Privacidad
                </a>{' '}
                (Ley 25.326 de Protección de Datos Personales)
              </label>
            </div>

            <button
              type="submit"
              disabled={isLoading || !form.termsAccepted}
              className="w-full rounded-md bg-primary py-2.5 font-medium text-ink-on-dark transition-colors disabled:opacity-50 hover:bg-primary-soft"
            >
              {isLoading ? 'Registrando…' : 'Crear cuenta'}
            </button>
          </form>
        </div>

        <p className="mt-6 text-center text-xs text-ink-faint">
          ¿Ya tienes cuenta?{' '}
          <a href="/login" className="underline hover:text-accent">
            Iniciar sesión
          </a>
        </p>
      </div>
    </div>
  );
}

