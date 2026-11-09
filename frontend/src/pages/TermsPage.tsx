/**
 * Términos y Condiciones de Uso — Nivex POS.
 * CONTENIDO PLACEHOLDER: debe ser revisado y aprobado por asesor legal
 * antes de la puesta en producción comercial.
 */
export default function TermsPage() {
  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="text-2xl font-semibold text-ink">
        Términos y Condiciones de Uso
      </h1>
      <p className="mt-2 text-xs text-ink-faint">
        Versión: borrador para revisión legal — Nivex POS
      </p>

      <div className="mt-6 space-y-5 text-sm leading-relaxed text-ink-soft">
        <section>
          <h2 className="text-base font-semibold text-ink">1. Objeto</h2>
          <p className="mt-1">
            Estos términos regulan el uso del sistema Nivex POS (en adelante,
            "el Sistema"), una plataforma de gestión de punto de venta,
            inventario y facturación electrónica prestada al comercio
            licenciatario.
          </p>
        </section>

        <section>
          <h2 className="text-base font-semibold text-ink">
            2. Registro de usuarios y consentimiento
          </h2>
          <p className="mt-1">
            El acceso al Sistema requiere una cuenta de usuario habilitada por
            el administrador del comercio. Al registrarse, el usuario declara
            haber leído y aceptado estos Términos y la{' '}
            <a href="/privacidad" className="text-accent underline">
              Política de Privacidad
            </a>
            , prestando su consentimiento libre, expreso e informado conforme
            al art. 5 de la Ley 25.326.
          </p>
        </section>

        <section>
          <h2 className="text-base font-semibold text-ink">
            3. Uso correcto del Sistema
          </h2>
          <p className="mt-1">
            El usuario se compromete a utilizar el Sistema conforme a su
            función comercial, abstiniéndose de usarlo para fines ilícitos,
            intentar accesos no autorizados o alterar registros contables y
            fiscales.
          </p>
        </section>

        <section>
          <h2 className="text-base font-semibold text-ink">
            4. Comprobantes fiscales
          </h2>
          <p className="mt-1">
            La emisión de comprobantes electrónicos se realiza mediante
            integración con los servicios de ARCA (ex AFIP). El comercio
            licenciatario es responsable de la veracidad de los datos
            facturados y del mantenimiento de su certificado digital.
          </p>
        </section>

        <section>
          <h2 className="text-base font-semibold text-ink">
            5. Responsabilidad y disponibilidad
          </h2>
          <p className="mt-1">
            El Sistema se presta "tal cual", con copias de seguridad periódicas.
            No se garantiza disponibilidad ininterrumpida ante fallas de
            conectividad, hardware o servicios de terceros.
          </p>
        </section>

        <section>
          <h2 className="text-base font-semibold text-ink">6. Ley aplicable</h2>
          <p className="mt-1">
            Estos términos se rigen por las leyes de la República Argentina.
            Cualquier controversia se somete a la jurisdicción de los tribunales
            ordinarios competentes.
          </p>
        </section>
      </div>
    </div>
  );
}