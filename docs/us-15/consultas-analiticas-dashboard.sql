-- US-15: consultas analíticas para dashboard visual
-- Objetivo: calcular ventas, demanda por talla, productos populares y palabras más buscadas.

-- 1) Ventas totales y pedidos del periodo
SELECT
    COALESCE(SUM(li.subtotal), 0) AS total_ventas,
    COUNT(DISTINCT p.id) AS total_pedidos,
    COUNT(li.id) AS total_lineas,
    ROUND(COALESCE(SUM(li.subtotal), 0) / NULLIF(COUNT(DISTINCT p.id), 0), 2) AS ticket_promedio
FROM pedidos p
JOIN pedido_items li ON li.pedido_id = p.id
WHERE p.fecha_creacion >= :startDate
  AND p.fecha_creacion < DATE_ADD(:endDate, INTERVAL 1 DAY)
  AND p.estado IN ('PAGADO', 'ENVIADO', 'ENTREGADO');

-- 2) Ventas por categoría
SELECT
    pr.categoria AS categoria,
    SUM(li.subtotal) AS ingreso_total,
    COUNT(li.id) AS unidades_vendidas
FROM pedido_items li
JOIN prendas pr ON pr.id = li.prenda_id
JOIN pedidos p ON p.id = li.pedido_id
WHERE p.fecha_creacion >= :startDate
  AND p.fecha_creacion < DATE_ADD(:endDate, INTERVAL 1 DAY)
  AND p.estado IN ('PAGADO', 'ENVIADO', 'ENTREGADO')
  AND (:categoryId IS NULL OR pr.categoria_id = :categoryId)
GROUP BY pr.categoria
ORDER BY ingreso_total DESC;

-- 3) Demanda por talla
SELECT
    li.talla AS talla,
    SUM(li.cantidad) AS unidades_vendidas,
    SUM(li.subtotal) AS ingreso_total
FROM pedido_items li
JOIN pedidos p ON p.id = li.pedido_id
WHERE p.fecha_creacion >= :startDate
  AND p.fecha_creacion < DATE_ADD(:endDate, INTERVAL 1 DAY)
  AND p.estado IN ('PAGADO', 'ENVIADO', 'ENTREGADO')
  AND (:categoryId IS NULL OR li.categoria_id = :categoryId)
GROUP BY li.talla
ORDER BY unidades_vendidas DESC;

-- 4) Productos más populares
SELECT
    pr.id,
    pr.nombre,
    SUM(li.cantidad) AS unidades_vendidas,
    SUM(li.subtotal) AS ingreso_total
FROM pedido_items li
JOIN prendas pr ON pr.id = li.prenda_id
JOIN pedidos p ON p.id = li.pedido_id
WHERE p.fecha_creacion >= :startDate
  AND p.fecha_creacion < DATE_ADD(:endDate, INTERVAL 1 DAY)
  AND p.estado IN ('PAGADO', 'ENVIADO', 'ENTREGADO')
  AND (:categoryId IS NULL OR pr.categoria_id = :categoryId)
GROUP BY pr.id, pr.nombre
ORDER BY unidades_vendidas DESC
LIMIT 10;

-- 5) Palabras más buscadas
SELECT
    b.termino AS palabra,
    SUM(b.frecuencia) AS total_busquedas
FROM busquedas b
WHERE b.fecha_busqueda >= :startDate
  AND b.fecha_busqueda < DATE_ADD(:endDate, INTERVAL 1 DAY)
  AND (:categoryId IS NULL OR b.categoria_id = :categoryId)
GROUP BY b.termino
ORDER BY total_busquedas DESC
LIMIT 10;

-- 6) Inventario total y stock bajo
SELECT
    COUNT(*) AS total_productos,
    SUM(t.stock) AS stock_total,
    SUM(CASE WHEN t.stock <= 5 THEN 1 ELSE 0 END) AS stock_bajo
FROM prendas p
JOIN talla_prenda t ON t.prenda_id = p.id
WHERE (:categoryId IS NULL OR p.categoria_id = :categoryId)
  AND p.estado = 'ACTIVA';

-- 7) Totales por vendedor (vista específica del vendedor)
SELECT
    v.id AS vendedor_id,
    SUM(li.subtotal) AS ventas_vendedor,
    COUNT(DISTINCT p.id) AS pedidos_vendedor,
    SUM(li.cantidad) AS unidades_vendidas
FROM pedidos p
JOIN pedido_items li ON li.pedido_id = p.id
JOIN prendas pr ON pr.id = li.prenda_id
JOIN usuarios v ON v.id = pr.vendedor_id
WHERE p.fecha_creacion >= :startDate
  AND p.fecha_creacion < DATE_ADD(:endDate, INTERVAL 1 DAY)
  AND v.id = :vendedorId
GROUP BY v.id;

-- 8) Variante con agrupación por día
SELECT
    DATE(p.fecha_creacion) AS fecha,
    SUM(li.subtotal) AS ventas_diarias,
    COUNT(DISTINCT p.id) AS pedidos_diarios
FROM pedidos p
JOIN pedido_items li ON li.pedido_id = p.id
WHERE p.fecha_creacion >= :startDate
  AND p.fecha_creacion < DATE_ADD(:endDate, INTERVAL 1 DAY)
GROUP BY DATE(p.fecha_creacion)
ORDER BY fecha ASC;

