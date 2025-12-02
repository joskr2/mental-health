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

-- === Bajar el umbral de similitud para ser más tolerante ===
-- Por defecto es 0.3. Lo bajamos a 0.1 para que "Gonsales" encuentre "González"
-- Usamos current_database() para que funcione con cualquier nombre de BD
DO $$
BEGIN
    EXECUTE format('ALTER DATABASE %I SET pg_trgm.similarity_threshold = 0.1', current_database());
END $$;

-- Log de inicialización
DO $$
BEGIN
    RAISE NOTICE 'Base de datos mental_clinic inicializada correctamente';
    RAISE NOTICE 'Extensiones instaladas: uuid-ossp, pg_trgm, btree_gist';
    RAISE NOTICE 'pg_trgm.similarity_threshold configurado a 0.1';
END $$;
