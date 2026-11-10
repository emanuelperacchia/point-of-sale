-- =========================================================================
-- V4: Agregar soporte de consentimiento (Términos y Privacidad) y Supresión
-- Cumplimiento Ley 25.326 (Protección de Datos Personales)
-- =========================================================================

ALTER TABLE users ADD COLUMN terms_accepted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN terms_accepted_at TIMESTAMP;
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP;
