-- US-15: Consultas analíticas corregidas para dashboard visual
-- Ruta del archivo: docs/us-15/consultas-analiticas-dashboard.sql

-- ============================================================================
-- 0) TABLAS Y MIGRACIONES DE SOPORTE E ÍNDICES (RNF-04 / RNF-15)
-- ============================================================================

-- Índices de rendimiento para evitar Table Scans
CREATE INDEX IF NOT EXISTS idx_pedidos_fecha_estado ON pedidos (fecha_creacion, estado);
CREATE INDEX IF NOT EXISTS idx_pedido_items_pedido_prenda ON pedido_items (pedido_id, prenda_id);
CREATE INDEX IF NOT EXISTS idx_prendas_vendedor_cat ON prendas (vendedor_id, categoria_id);
CREATE INDEX IF NOT EXISTS idx_talla_prenda_stock ON talla_prenda (prenda_id, stock);

-- Tabla para tracking real de búsquedas
CREATE TABLE IF NOT EXISTS busquedas (
                                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                         termino VARCHAR(100) NOT NULL,
    categoria_id BIGINT NULL,
    fecha_busqueda DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (categoria_id) REFERENCES categorias(id) ON DELETE SET NULL
    );
CREATE INDEX IF NOT EXISTS idx_busquedas_fecha_cat ON busquedas (fecha_busqueda, categoria_id);

-- Tabla para métricas reales de "Productos Más Consultados"
CREATE TABLE IF NOT EXISTS prenda_vistas (
                                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                             prenda_id BIGINT NOT NULL,
                                             fecha_vista DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                             FOREIGN KEY (prenda_id) REFERENCES prendas(id) ON DELETE CASCADE
    );
CREATE INDEX IF NOT EXISTS idx_prenda_vistas_prenda_fecha ON prenda_vistas (prenda_id, fecha_vista);

-- ============================================================================
-- 1) VENTAS TOTALES, PEDIDOS Y TICKET PROMEDIO (Con aislamiento por Vendedor)
-- ============================================================================
SELECT
    COALESCE(SUM(li.subtotal), 0) AS total_ventas,
    COUNT(DISTINCT p.id) AS total_pedidos,
    COALESCE(SUM(li.cantidad), 0) AS productos_vendidos,
    ROUND(COALESCE(SUM(li.subtotal), 0) / NULLIF(COUNT(DISTINCT p.id), 0), 2) AS ticket_promedio
FROM pedidos p
         JOIN pedido_items li ON li.pedido_id = p.id
         JOIN prendas pr ON pr.id = li.prenda_id
WHERE p.fecha_creacion >= :startDate
  AND p.fecha_creacion < DATE_ADD(:endDate, INTERVAL 1 DAY)
  AND p.estado IN ('PAGADO', 'ENVIADO', 'ENTREGADO')
  AND (:vendedorId IS NULL OR pr.vendedor_id = :vendedorId)
  AND (:categoryId IS NULL OR pr.categoria_id = :categoryId);

-- ============================================================================
-- 2) VENTAS POR CATEGORÍA
-- ============================================================================
SELECT
    c.nombre AS categoria,
    COALESCE(SUM(li.subtotal), 0) AS ingreso_total,
    COALESCE(SUM(li.cantidad), 0) AS unidades_vendidas
FROM pedido_items li
         JOIN prendas pr ON pr.id = li.prenda_id
         JOIN categorias c ON c.id = pr.categoria_id
         JOIN pedidos p ON p.id = li.pedido_id
WHERE p.fecha_creacion >= :startDate
  AND p.fecha_creacion < DATE_ADD(:endDate, INTERVAL 1 DAY)
  AND p.estado IN ('PAGADO', 'ENVIADO', 'ENTREGADO')
  AND (:vendedorId IS NULL OR pr.vendedor_id = :vendedorId)
  AND (:categoryId IS NULL OR pr.categoria_id = :categoryId)
GROUP BY c.id, c.nombre
ORDER BY ingreso_total DESC;

-- ============================================================================
-- 3) DEMANDA POR TALLA
-- ============================================================================
SELECT
    li.talla AS talla,
    COALESCE(SUM(li.cantidad), 0) AS unidades_vendidas,
    COALESCE(SUM(li.subtotal), 0) AS ingreso_total
FROM pedido_items li
         JOIN prendas pr ON pr.id = li.prenda_id
         JOIN pedidos p ON p.id = li.pedido_id
WHERE p.fecha_creacion >= :startDate
  AND p.fecha_creacion < DATE_ADD(:endDate, INTERVAL 1 DAY)
  AND p.estado IN ('PAGADO', 'ENVIADO', 'ENTREGADO')
  AND (:vendedorId IS NULL OR pr.vendedor_id = :vendedorId)
  AND (:categoryId IS NULL OR pr.categoria_id = :categoryId)
GROUP BY li.talla
ORDER BY unidades_vendidas DESC;

-- ============================================================================
-- 4) PRODUCTOS MÁS VENDIDOS (TOP 10)
-- ============================================================================
SELECT
    pr.id,
    pr.nombre,
    COALESCE(SUM(li.cantidad), 0) AS unidades_vendidas,
    COALESCE(SUM(li.subtotal), 0) AS ingreso_total
FROM pedido_items li
         JOIN prendas pr ON pr.id = li.prenda_id
         JOIN pedidos p ON p.id = li.pedido_id
WHERE p.fecha_creacion >= :startDate
  AND p.fecha_creacion < DATE_ADD(:endDate, INTERVAL 1 DAY)
  AND p.estado IN ('PAGADO', 'ENVIADO', 'ENTREGADO')
  AND (:vendedorId IS NULL OR pr.vendedor_id = :vendedorId)
  AND (:categoryId IS NULL OR pr.categoria_id = :categoryId)
GROUP BY pr.id, pr.nombre
ORDER BY unidades_vendidas DESC
    LIMIT 10;

-- ============================================================================
-- 5) PRODUCTOS MÁS CONSULTADOS (Vistas Reales en prenda_vistas)
-- ============================================================================
SELECT
    pr.id,
    pr.nombre,
    COUNT(v.id) AS total_visitas
FROM prenda_vistas v
         JOIN prendas pr ON pr.id = v.prenda_id
WHERE v.fecha_vista >= :startDate
  AND v.fecha_vista < DATE_ADD(:endDate, INTERVAL 1 DAY)
  AND (:vendedorId IS NULL OR pr.vendedor_id = :vendedorId)
  AND (:categoryId IS NULL OR pr.categoria_id = :categoryId)
GROUP BY pr.id, pr.nombre
ORDER BY total_visitas DESC
    LIMIT 10;

-- ============================================================================
-- 6) PALABRAS MÁS BUSCADAS (Búsquedas Reales en busquedas)
-- ============================================================================
SELECT
    b.termino AS palabra,
    COUNT(b.id) AS total_busquedas
FROM busquedas b
WHERE b.fecha_busqueda >= :startDate
  AND b.fecha_busqueda < DATE_ADD(:endDate, INTERVAL 1 DAY)
  AND (:categoryId IS NULL OR b.categoria_id = :categoryId)
GROUP BY b.termino
ORDER BY total_busquedas DESC
    LIMIT 10;

-- ============================================================================
-- 7) INVENTARIO TOTAL Y STOCK BAJO
-- ============================================================================
SELECT
    COUNT(DISTINCT p.id) AS total_productos,
    COALESCE(SUM(t.stock), 0) AS stock_total,
    COALESCE(SUM(CASE WHEN t.stock <= 5 THEN 1 ELSE 0 END), 0) AS stock_bajo
FROM prendas p
         JOIN talla_prenda t ON t.prenda_id = p.id
WHERE p.estado = 'ACTIVA'
  AND (:vendedorId IS NULL OR p.vendedor_id = :vendedorId)
  AND (:categoryId IS NULL OR p.categoria_id = :categoryId);

-- ============================================================================
-- 8) EVOLUCIÓN DE VENTAS DIARIAS
-- ============================================================================
SELECT
    DATE(p.fecha_creacion) AS fecha,
    COALESCE(SUM(li.subtotal), 0) AS ventas_diarias,
    COUNT(DISTINCT p.id) AS pedidos_diarios
FROM pedidos p
    JOIN pedido_items li ON li.pedido_id = p.id
    JOIN prendas pr ON pr.id = li.prenda_id
WHERE p.fecha_creacion >= :startDate
  AND p.fecha_creacion < DATE_ADD(:endDate, INTERVAL 1 DAY)
  AND p.estado IN ('PAGADO', 'ENVIADO', 'ENTREGADO')
  AND (:vendedorId IS NULL OR pr.vendedor_id = :vendedorId)
  AND (:categoryId IS NULL OR pr.categoria_id = :categoryId)