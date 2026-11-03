-- US-15 — Consultas analíticas del dashboard
-- Referencia de solo lectura contra el esquema vigente (MySQL 8 / H2 en modo MySQL).
-- La implementación real es el JPQL de `DashboardAnalyticsRepository`; este archivo sirve para
-- verificar los números y para explicar qué agrega cada métrica.
--
-- Parámetros:
--   :inicio, :fin  DATETIME con rango semiabierto [inicio, fin)
--   :vendedorId    NULL = consolidado (administrador); id = tienda del vendedor
--   :categoria     NULL = todas; si viene, código del enum (`VESTIDOS`, `BUZOS`, ...)
--
-- Estados que cuentan como venta: PAGADO, REGISTRADO, EN_PREPARACION,
-- PARCIALMENTE_ENVIADO y ENVIADO. Fuera: PENDIENTE_PAGO y CANCELADO.

-- ============================================================================
-- 0) SOPORTE E ÍNDICES (RNF-04). En prod, ejecutar `migracion-analiticas.sql`
--    antes del despliegue; en dev/local los crea Hibernate.
-- ============================================================================

-- CREATE TABLE busquedas (id, termino, total_resultados, fecha);
-- CREATE TABLE producto_vistas (id, prenda_id, fecha);

CREATE INDEX IF NOT EXISTS idx_pedidos_fecha_estado ON pedidos (fecha_creacion, estado);
CREATE INDEX IF NOT EXISTS idx_pedido_items_prenda ON pedido_items (prenda_id);

-- En `prendas` ya existen índices por vendedor, estado, categoría y fecha.
-- En `prenda_tallas` el acceso es por prenda; el stock se mide por variante.

-- ============================================================================
-- 1) VENTAS, PEDIDOS, TICKET Y UNIDADES DEL PERÍODO
-- ============================================================================

SELECT
    COALESCE(SUM(li.subtotal), 0)                                      AS total_ventas,
    COUNT(DISTINCT p.id)                                               AS total_pedidos,
    COALESCE(SUM(li.cantidad), 0)                                      AS productos_vendidos,
    ROUND(COALESCE(SUM(li.subtotal), 0) / NULLIF(COUNT(DISTINCT p.id), 0), 2) AS ticket_promedio
FROM pedidos p
         JOIN pedido_items li ON li.pedido_id = p.id
         JOIN prendas pr ON pr.id = li.prenda_id
WHERE p.fecha_creacion >= :inicio
  AND p.fecha_creacion < :fin
  AND p.estado IN ('PAGADO', 'REGISTRADO', 'EN_PREPARACION', 'PARCIALMENTE_ENVIADO', 'ENVIADO')
  AND (:vendedorId IS NULL OR pr.vendedor_id = :vendedorId)
  AND (:categoria IS NULL OR pr.categoria = :categoria);

-- ============================================================================
-- 2) VENTAS POR CATEGORÍA (monto y unidades)
-- La categoría es un VARCHAR del enum `CategoriaPrenda` dentro de `prendas`.
-- ============================================================================

SELECT
    pr.categoria                                                       AS categoria,
    COALESCE(SUM(li.subtotal), 0)                                      AS ingreso_total,
    COALESCE(SUM(li.cantidad), 0)                                      AS unidades_vendidas
FROM pedido_items li
         JOIN pedidos p ON p.id = li.pedido_id
         JOIN prendas pr ON pr.id = li.prenda_id
WHERE p.fecha_creacion >= :inicio
  AND p.fecha_creacion < :fin
  AND p.estado IN ('PAGADO', 'REGISTRADO', 'EN_PREPARACION', 'PARCIALMENTE_ENVIADO', 'ENVIADO')
  AND (:vendedorId IS NULL OR pr.vendedor_id = :vendedorId)
  AND (:categoria IS NULL OR pr.categoria = :categoria)
GROUP BY pr.categoria
ORDER BY ingreso_total DESC;

-- ============================================================================
-- 3) DEMANDA POR TALLA
-- Se agrupa solo por `talla`: la pregunta es qué tamaño se vende más. El color
-- se conserva en `pedido_items.color` para auditoría, pero no se separa aquí.
-- Para el detalle por prenda y talla, ver la consulta 9.
-- ============================================================================

SELECT
    li.talla                                                           AS talla,
    COALESCE(SUM(li.cantidad), 0)                                      AS unidades_vendidas,
    COALESCE(SUM(li.subtotal), 0)                                      AS ingreso_total
FROM pedido_items li
         JOIN pedidos p ON p.id = li.pedido_id
         JOIN prendas pr ON pr.id = li.prenda_id
WHERE p.fecha_creacion >= :inicio
  AND p.fecha_creacion < :fin
  AND p.estado IN ('PAGADO', 'REGISTRADO', 'EN_PREPARACION', 'PARCIALMENTE_ENVIADO', 'ENVIADO')
  AND (:vendedorId IS NULL OR pr.vendedor_id = :vendedorId)
  AND (:categoria IS NULL OR pr.categoria = :categoria)
GROUP BY li.talla
ORDER BY unidades_vendidas DESC, li.talla ASC
LIMIT 10;

-- ============================================================================
-- 4) PRODUCTOS MÁS VENDIDOS (TOP 10)
-- ============================================================================

SELECT
    pr.id,
    pr.nombre,
    COALESCE(SUM(li.cantidad), 0)                                      AS unidades_vendidas,
    COALESCE(SUM(li.subtotal), 0)                                      AS ingreso_total
FROM pedido_items li
         JOIN pedidos p ON p.id = li.pedido_id
         JOIN prendas pr ON pr.id = li.prenda_id
WHERE p.fecha_creacion >= :inicio
  AND p.fecha_creacion < :fin
  AND p.estado IN ('PAGADO', 'REGISTRADO', 'EN_PREPARACION', 'PARCIALMENTE_ENVIADO', 'ENVIADO')
  AND (:vendedorId IS NULL OR pr.vendedor_id = :vendedorId)
  AND (:categoria IS NULL OR pr.categoria = :categoria)
GROUP BY pr.id, pr.nombre
ORDER BY unidades_vendidas DESC, pr.nombre ASC
LIMIT 10;

-- ============================================================================
-- 5) PRODUCTOS MÁS CONSULTADOS (VISTAS REALES DE LA FICHA)
-- `producto_vistas` no guarda quién vio: solo prenda y fecha.
-- ============================================================================

SELECT
    pr.id,
    pr.nombre,
    COUNT(v.id)                                                        AS total_consultas
FROM producto_vistas v
         JOIN prendas pr ON pr.id = v.prenda_id
WHERE v.fecha >= :inicio
  AND v.fecha < :fin
  AND (:vendedorId IS NULL OR pr.vendedor_id = :vendedorId)
  AND (:categoria IS NULL OR pr.categoria = :categoria)
GROUP BY pr.id, pr.nombre
ORDER BY total_consultas DESC, pr.nombre ASC
LIMIT 10;

-- ============================================================================
-- 6) PALABRAS MÁS BUSCADAS
-- Señal de toda la plataforma: una búsqueda no pertenece a un vendedor ni a una
-- categoría, por eso NO se filtran :vendedorId ni :categoria.
-- ============================================================================

SELECT
    b.termino                                                          AS palabra,
    COUNT(b.id)                                                        AS total_busquedas
FROM busquedas b
WHERE b.fecha >= :inicio
  AND b.fecha < :fin
GROUP BY b.termino
ORDER BY total_busquedas DESC, b.termino ASC
LIMIT 10;

-- ============================================================================
-- 7) INVENTARIO: TOTAL Y VARIANTES CON STOCK BAJO
-- Cada fila de `prenda_tallas` es una variante `prenda + talla + color`.
-- `stock_bajo` cuenta VARIANTES, no tallas distintas. Una variante sin
-- `stock_minimo` no alerta.
-- ============================================================================

SELECT
    COUNT(DISTINCT pr.id)                                              AS total_prendas,
    COALESCE(SUM(t.stock), 0)                                          AS stock_total,
    COALESCE(SUM(CASE
                     WHEN t.stock_minimo IS NOT NULL AND t.stock <= t.stock_minimo
                         THEN 1 ELSE 0 END), 0)                        AS variantes_stock_bajo
FROM prendas pr
         JOIN prenda_tallas t ON t.prenda_id = pr.id
WHERE pr.estado = 'ACTIVA'
  AND (:vendedorId IS NULL OR pr.vendedor_id = :vendedorId)
  AND (:categoria IS NULL OR pr.categoria = :categoria);

-- 7.1) Detalle por variante: una fila por prenda, talla y color.
SELECT
    pr.id                                                              AS prenda_id,
    pr.nombre                                                          AS prenda,
    t.talla,
    t.color,
    t.stock,
    t.stock_minimo,
    CASE
        WHEN t.stock = 0 THEN 'AGOTADO'
        WHEN t.stock_minimo IS NOT NULL AND t.stock <= t.stock_minimo THEN 'STOCK_BAJO'
        ELSE 'DISPONIBLE'
        END                                                            AS nivel
FROM prendas pr
         JOIN prenda_tallas t ON t.prenda_id = pr.id
WHERE pr.estado = 'ACTIVA'
  AND (:vendedorId IS NULL OR pr.vendedor_id = :vendedorId)
  AND (:categoria IS NULL OR pr.categoria = :categoria)
ORDER BY pr.nombre, t.talla, t.color;

-- ============================================================================
-- 8) EVOLUCIÓN DIARIA DE VENTAS
-- ============================================================================

SELECT
    DATE(p.fecha_creacion)                                             AS fecha,
    COALESCE(SUM(li.subtotal), 0)                                      AS ventas_diarias,
    COUNT(DISTINCT p.id)                                               AS pedidos_diarios
FROM pedidos p
         JOIN pedido_items li ON li.pedido_id = p.id
         JOIN prendas pr ON pr.id = li.prenda_id
WHERE p.fecha_creacion >= :inicio
  AND p.fecha_creacion < :fin
  AND p.estado IN ('PAGADO', 'REGISTRADO', 'EN_PREPARACION', 'PARCIALMENTE_ENVIADO', 'ENVIADO')
  AND (:vendedorId IS NULL OR pr.vendedor_id = :vendedorId)
  AND (:categoria IS NULL OR pr.categoria = :categoria)
GROUP BY DATE(p.fecha_creacion)
ORDER BY fecha;

-- ============================================================================
-- 9) DEMANDA DETALLADA POR PRENDA Y TALLA (reporte US-16, no gráfico)
-- Mantiene el color fuera de la agrupación y lo expone solo como dato de la
-- línea cuando se necesita auditar una venta concreta.
-- ============================================================================

SELECT
    pr.nombre                                                          AS prenda,
    li.talla,
    COALESCE(SUM(li.cantidad), 0)                                      AS unidades,
    COALESCE(SUM(li.subtotal), 0)                                      AS valor
FROM pedido_items li
         JOIN pedidos p ON p.id = li.pedido_id
         JOIN prendas pr ON pr.id = li.prenda_id
WHERE p.fecha_creacion >= :inicio
  AND p.fecha_creacion < :fin
  AND p.estado IN ('PAGADO', 'REGISTRADO', 'EN_PREPARACION', 'PARCIALMENTE_ENVIADO', 'ENVIADO')
  AND (:vendedorId IS NULL OR pr.vendedor_id = :vendedorId)
  AND (:categoria IS NULL OR pr.categoria = :categoria)
GROUP BY pr.id, pr.nombre, li.talla
ORDER BY unidades DESC, pr.nombre ASC, li.talla ASC;
