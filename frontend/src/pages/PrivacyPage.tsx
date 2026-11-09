import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { privacyApi } from '../services/api';
import Layout from '../components/common/Layout';

/**
 * Mi Perfil / Privacidad — Ley 25.326.
 * Derecho de acceso y portabilidad (art. 14): exportar datos personales.
 * Derecho de supresión (art. 16): solicitar la baja lógica de la cuenta.
 */
export default function PrivacyPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleExport = async () => {
    setError(null);
    try {
      const { data } = await privacyApi.exportMyData();
      const blob = new Blob([JSON.stringify(data, null, 2)], {
        type: 'application/json',
      });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `nivex-datos-${user?.email ?? 'usuario'}.json`;
      link.click();
      URL.revokeObjectURL(url);
    } catch {
      setError('No se pudieron exportar los datos. Intentalo de nuevo.');
    }
  };

  const handleDeleteRequest = async () => {
    setDeleting(true);
    setError(null);
    try {
      await privacyApi.requestAccountDeletion();
      setShowDeleteModal(false);
      await logout();
      navigate('/login', { replace: true });
    } catch {
      setError('No se pudo procesar la solicitud. Contactá al administrador.');
      setShowDeleteModal(false);
    } finally {
      setDeleting(false);
    }
  };

  return (
    <Layout title="Mi Perfil">
      <div className="mx-auto max-w-3xl space-y-6">
      <section className="rounded-xl bg-surface p-6 shadow-soft">
        <h3 className="text-base font-semibold text-ink">Mis datos</h3>
        <p className="mt-1 text-sm text-ink-soft">
          Información de tu cuenta en Nivex. Tus datos personales se tratan
          conforme a la Ley 25.326 de Protección de Datos Personales.
        </p>
        <dl className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
          <div>
            <dt className="text-xs font-medium uppercase tracking-wider text-ink-faint">
              Nombre
            </dt>
            <dd className="mt-0.5 text-sm text-ink">{user?.fullName ?? '—'}</dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wider text-ink-faint">
              Email
            </dt>
            <dd className="mt-0.5 text-sm text-ink">{user?.email ?? '—'}</dd>
          </div>
        </dl>
      </section>

      {error && (
        <div className="rounded-lg border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger">
          {error}
        </div>
      )}

      <section className="rounded-xl bg-surface p-6 shadow-soft">
        <h3 className="text-base font-semibold text-ink">
          Exportar mis datos (Portabilidad)
        </h3>
        <p className="mt-1 text-sm text-ink-soft">
          Descargá en formato JSON todos los datos personales que el sistema
          almacena sobre vos.
        </p>
        <button
          onClick={handleExport}
          className="mt-4 rounded-md bg-primary px-4 py-2 text-sm font-medium text-ink-on-dark transition-colors hover:bg-primary-soft"
        >
          Descargar mis datos
        </button>
      </section>

      <section className="rounded-xl border border-danger/30 bg-surface p-6 shadow-soft">
        <h3 className="text-base font-semibold text-danger">
          Solicitar baja de mi cuenta (Supresión)
        </h3>
        <p className="mt-1 text-sm text-ink-soft">
          Tu cuenta quedará desactivada y no podrás volver a ingresar. Los
          registros fiscales y contables asociados se conservan por obligación
          legal (RG AFIP), pero tus datos dejan de ser utilizados.
        </p>
        <button
          onClick={() => setShowDeleteModal(true)}
          className="mt-4 rounded-md bg-danger px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-danger/85"
        >
          Solicitar baja de mi cuenta
        </button>
      </section>

      {showDeleteModal && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
          role="dialog"
          aria-modal="true"
        >
          <div className="mx-4 w-full max-w-md rounded-xl bg-surface p-6 shadow-soft">
            <h4 className="text-base font-semibold text-ink">
              ¿Confirmás la baja de tu cuenta?
            </h4>
            <p className="mt-2 text-sm text-ink-soft">
              Esta acción desactiva tu acceso de forma permanente. Cerrarás tu
              sesión al confirmar.
            </p>
            <div className="mt-5 flex justify-end gap-3">
              <button
                onClick={() => setShowDeleteModal(false)}
                className="rounded-md border border-ink-faint/30 px-4 py-2 text-sm text-ink transition-colors hover:bg-background"
                disabled={deleting}
              >
                Cancelar
              </button>
              <button
                onClick={handleDeleteRequest}
                className="rounded-md bg-danger px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-danger/85"
                disabled={deleting}
              >
                {deleting ? 'Procesando…' : 'Confirmar baja'}
              </button>
            </div>
          </div>
        </div>
      )}
      </div>
    </Layout>
  );
}