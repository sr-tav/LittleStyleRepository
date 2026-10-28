-- US-15: tablas e índices del dashboard analítico.
-- En dev/local Hibernate (ddl-auto=update) los crea solo. En prod (ddl-auto=validate) hay que ejecutar
-- este script ANTES de desplegar, o la aplicación no arranca. Sintaxis MySQL.

CREATE TABLE IF NOT EXISTS busquedas (
    id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    termino           VARCHAR(120) NOT NULL,
    total_resultados  BIGINT       NOT NULL,
    fecha             DATETIME(6)  NOT NULL,
    INDEX idx_busquedas_fecha_termino (fecha, termino)
);

CREATE TABLE IF NOT EXISTS producto_vistas (
    id         BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    prenda_id  BIGINT      NOT NULL,
    fecha      DATETIME(6) NOT NULL,
    INDEX idx_producto_vistas_prenda_fecha (prenda_id, fecha),
    INDEX idx_producto_vistas_fecha (fecha)
);

-- Índices sobre tablas existentes: todas las consultas del dashboard filtran por fecha y estado.
CREATE INDEX idx_pedidos_fecha_estado ON pedidos (fecha_creacion, estado);
CREATE INDEX idx_pedido_items_prenda ON pedido_items (prenda_id);
