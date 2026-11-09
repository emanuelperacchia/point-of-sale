/**
 * Política de Privacidad — Nivex POS.
 * CONTENIDO PLACEHOLDER: debe ser revisado y aprobado por asesor legal
 * (Ley 25.326 de Protección de Datos Personales, Argentina).
 */
export default function PrivacyPolicyPage() {
  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="text-2xl font-semibold text-ink">Política de Privacidad</h1>
      <p className="mt-2 text-xs text-ink-faint">
        Versión: borrador para revisión legal — Ley 25.326 — Nivex POS
      </p>

      <div className="mt-6 space-y-5 text-sm leading-relaxed text-ink-soft">
        <section>
          <h2 className="text-base font-semibold text-ink">
            1. Responsable del tratamiento
          </h2>
          <p className="mt-1">
            El responsable del tratamiento de los datos personales es el
            comercio licenciatario del Sistema Nivex POS, quien determina las
            finalidades y medios de procesamiento. [COMPLETAR: razón social,
            CUIT y domicilio.]
          </p>
        </section>

        <section>
          <h2 className="text-base font-semibold text-ink">
            2. Datos que se recaban
          </h2>
          <p className="mt-1">
            Usuarios del sistema: nombre, apellido, email, teléfono, fecha y
            hora de accesos, y aceptación de términos. Clientes y empleados del
            comercio: datos fiscales y documentación cargada por el comercio
            (p. ej. DNI, legajos). No se recaban datos biométricos ni
            financieros.
          </p>
        </section>

        <section>
          <h2 className="text-base font-semibold text-ink">
            3. Finalidad del tratamiento
          </h2>
          <p className="mt-1">
            Los datos se utilizan exclusivamente para la operación del punto de
            venta: facturación electrónica ante ARCA, control de inventario,
            liquidaciones de sueldo, reportes comerciales y auditoría de
            seguridad. No se ceden ni venden datos a terceros con fines
            publicitarios.
          </p>
        </section>

        <section>
          <h2 className="text-base font-semibold text-ink">
            4. Derechos del titular (arts. 14, 16 y 17, Ley 25.326)
          </h2>
          <p className="mt-1">
            Podés ejercer tus derechos de acceso y portabilidad desde la sección
            "Mi perfil" del Sistema, donde podés descargar tus datos personales.
            También podés solicitar la supresión de tu cuenta: la solicitud se
            procesa con baja lógica, preservando únicamente los registros que
            por obligación legal (normativa fiscal y laboral) deban
            conservarse.
          </p>
        </section>

        <section>
          <h2 className="text-base font-semibold text-ink">5. Seguridad</h2>
          <p className="mt-1">
            El Sistema aplica medidas técnicas y organizativas razonables:
            cifrado de secretos (AES-256-GCM), almacenamiento de contraseñas
            con hash (BCrypt), tokens de sesión con expiración y control de
            acceso por roles, conforme al art. 9 de la Ley 25.326.
          </p>
        </section>

        <section>
          <h2 className="text-base font-semibold text-ink">
            6. Conservación y supresión
          </h2>
          <p className="mt-1">
            Los comprobantes fiscales y registros contables se conservan por el
            plazo legal vigente (10 años, Código de Comercio). Los datos
            personales no sujetos a obligación legal se suprimen al solicitar
            la baja de la cuenta.
          </p>
        </section>

        <section>
          <h2 className="text-base font-semibold text-ink">7. Contacto</h2>
          <p className="mt-1">
            Para consultas sobre el tratamiento de tus datos: [COMPLETAR email
            de contacto del responsable].
          </p>
        </section>
      </div>
    </div>
  );
}