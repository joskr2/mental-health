-- ============================================
-- Script de Inicialización PostgreSQL
-- ============================================
-- Este script se ejecuta automáticamente cuando
-- el contenedor de PostgreSQL se inicia por primera vez.
-- ============================================

-- Extensión para UUIDs (útil para futuras mejoras)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Extensión para funciones de texto avanzadas
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Extensión btree_gist para constraints de exclusión temporal
-- IMPORTANTE: Necesaria para prevenir race conditions en citas
CREATE EXTENSION IF NOT EXISTS "btree_gist";

-- Configuración de timezone
SET timezone = 'America/Lima';

-- Log de inicialización
DO $$
BEGIN
    RAISE NOTICE 'Base de datos mental_clinic inicializada correctamente';
    RAISE NOTICE 'Extensiones instaladas: uuid-ossp, pg_trgm, btree_gist';
END $$;
