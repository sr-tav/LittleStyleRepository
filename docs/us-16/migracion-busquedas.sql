-- Migración de la tabla de búsquedas del catálogo (US-16).
-- Ejecutar una sola vez sobre bases existentes ANTES de desplegar el cambio.
-- En local (H2 en archivo con ddl-auto=update) basta borrar
-- data/littlestyle-local.mv.db y dejar que Hibernate regenere el esquema.
-- En MySQL/MariaDB (prod) ejecutar este script.

-- 1. Registro anónimo de búsquedas. No guarda nombre, correo ni dirección:
--    usuario_id es el único dato personal y es opcional.
CREATE TABLE IF NOT EXISTS busqueda_catalogo (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  termino_normalizado VARCHAR(200) NOT NULL,
  termino_original VARCHAR(200) NOT NULL,
  categoria VARCHAR(30) NULL,
  resultados_count INT NOT NULL,
  usuario_id BIGINT NULL,
  creado_en DATETIME NOT NULL
);

-- 2. Índices que usan el reporte y la limpieza por fecha.
--    Los nombres coinciden con los declarados en la entidad BusquedaCatalogo.
CREATE INDEX idx_busqueda_termino ON busqueda_catalogo (termino_normalizado);
CREATE INDEX idx_busqueda_fecha ON busqueda_catalogo (creado_en);
CREATE INDEX idx_busqueda_categoria ON busqueda_catalogo (categoria);

-- 3. No se crea FK contra usuarios a propósito: el histórico de búsquedas debe sobrevivir
--    a la baja de una cuenta. El id se conserva como referencia débil y puede ser NULL.
--    Si la política de privacidad exige borrar el historial al eliminar la cuenta, hacerlo
--    con un DELETE explícito por usuario_id antes de borrar el usuario.

-- 4. Verificación rápida después de migrar.
-- SELECT COUNT(*) AS total,
--        SUM(CASE WHEN resultados_count = 0 THEN 1 ELSE 0 END) AS sin_resultados
-- FROM busqueda_catalogo;
