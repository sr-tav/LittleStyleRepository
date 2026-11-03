-- US-16: consultas de verificación de los datos que alimentan los reportes
-- Motor: MySQL 8.4. Las consultas replican, en SQL, las mismas reglas que aplican
-- ReporteBusquedaRepository, ReporteInventarioRepository y DashboardAnalyticsRepository.
--
-- La tasa de éxito del RNF-24 NO vive en la base de datos: se mide con Micrometer en
-- memoria y se consulta en GET /api/v1/reportes/metricas/exito o en /actuator/metrics.
-- Por eso este archivo no incluye una consulta de exportaciones.

-- Parámetros usados en las consultas:
--   :startDate  DATE        inicio del período (inclusive)
--   :endDate    DATE        fin del período (inclusive)
--   :categoria  VARCHAR(20) categoría del enum CategoriaPrenda o NULL
--   :vendedorId BIGINT      vendedor o NULL para el consolidado global
--
-- Estados con pago confirmado: PAGADO, REGISTRADO, EN_PREPARACION,
-- PARCIALMENTE_ENVIADO y ENVIADO. Se excluyen PENDIENTE_PAGO y CANCELADO.

-- ---------------------------------------------------------------------------
-- 1) Búsquedas: resumen por término (equivale al reporte BUSQUEDAS del administrador)
--    Columnas: término, búsquedas, sin resultados, última búsqueda.
-- ---------------------------------------------------------------------------
SELECT
    MIN(b.termino_original) AS termino,
    COUNT(*) AS busquedas,
    SUM(CASE WHEN b.resultados_count = 0 THEN 1 ELSE 0 END) AS sin_resultados,
    MAX(b.creado_en) AS ultima_busqueda
FROM busqueda_catalogo b
WHERE b.creado_en >= :startDate
  AND b.creado_en < DATE_ADD(:endDate, INTERVAL 1 DAY)
  AND (:categoria IS NULL OR b.categoria = :categoria)
GROUP BY b.termino_normalizado
ORDER BY busquedas DESC, ultima_busqueda DESC;

-- ---------------------------------------------------------------------------
-- 2) Búsquedas sin resultados (demanda insatisfecha)
-- ---------------------------------------------------------------------------
SELECT
    b.termino_original AS termino,
    b.categoria,
    COUNT(*) AS veces_sin_resultados,
    MAX(b.creado_en) AS ultima_busqueda
FROM busqueda_catalogo b
WHERE b.creado_en >= :startDate
  AND b.creado_en < DATE_ADD(:endDate, INTERVAL 1 DAY)
  AND b.resultados_count = 0
  AND (:categoria IS NULL OR b.categoria = :categoria)
GROUP BY b.termino_original, b.categoria
ORDER BY veces_sin_resultados DESC
LIMIT 20;

-- ---------------------------------------------------------------------------
-- 3) Cobertura de búsquedas: proporción de búsquedas que sí encontraron algo
-- ---------------------------------------------------------------------------
SELECT
    COUNT(*) AS total_busquedas,
    SUM(CASE WHEN b.resultados_count > 0 THEN 1 ELSE 0 END) AS con_resultados,
    SUM(CASE WHEN b.resultados_count = 0 THEN 1 ELSE 0 END) AS sin_resultados,
    ROUND(
        100 * SUM(CASE WHEN b.resultados_count > 0 THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0),
        2
    ) AS cobertura_pct
FROM busqueda_catalogo b
WHERE b.creado_en >= :startDate
  AND b.creado_en < DATE_ADD(:endDate, INTERVAL 1 DAY)
  AND (:categoria IS NULL OR b.categoria = :categoria);

-- ---------------------------------------------------------------------------
-- 4) Búsquedas visibles para un vendedor
--    El vendedor solo ve búsquedas sin resultados en categorías que vende,
--    porque no se persisten los ids de las prendas devueltas.
-- ---------------------------------------------------------------------------
SELECT
    b.termino_original AS termino,
    b.categoria,
    COUNT(*) AS veces_sin_resultados,
    MAX(b.creado_en) AS ultima_busqueda
FROM busqueda_catalogo b
WHERE b.creado_en >= :startDate
  AND b.creado_en < DATE_ADD(:endDate, INTERVAL 1 DAY)
  AND b.resultados_count = 0
  AND EXISTS (
      SELECT 1
      FROM prendas p
      WHERE p.vendedor_id = :vendedorId
        AND p.categoria = b.categoria
  )
  AND (:categoria IS NULL OR b.categoria = :categoria)
GROUP BY b.termino_original, b.categoria
ORDER BY veces_sin_resultados DESC;

-- ---------------------------------------------------------------------------
-- 5) Ventas por día (serie del reporte VENTAS y nivel de barra)
--    El nivel de barra del PDF es round(10 * total_dia / max_dia), acotado a 0..10.
-- ---------------------------------------------------------------------------
WITH diarias AS (
    SELECT
        DATE(p.fecha_creacion) AS fecha,
        COUNT(DISTINCT p.id) AS pedidos,
        COALESCE(SUM(li.subtotal), 0) AS total
    FROM pedidos p
    JOIN pedido_items li ON li.pedido_id = p.id
    JOIN prendas pr ON pr.id = li.prenda_id
    WHERE p.fecha_creacion >= :startDate
      AND p.fecha_creacion < DATE_ADD(:endDate, INTERVAL 1 DAY)
      AND p.estado IN ('PAGADO', 'REGISTRADO', 'EN_PREPARACION', 'PARCIALMENTE_ENVIADO', 'ENVIADO')
      AND (:categoria IS NULL OR pr.categoria = :categoria)
      AND (:vendedorId IS NULL OR pr.vendedor_id = :vendedorId)
    GROUP BY DATE(p.fecha_creacion)
)
SELECT
    fecha,
    pedidos,
    total,
    CASE
        WHEN MAX(total) OVER () = 0 THEN 0
        ELSE LEAST(10, GREATEST(0, ROUND(10 * total / MAX(total) OVER ())))
    END AS nivel_barra
FROM diarias
ORDER BY fecha;

-- ---------------------------------------------------------------------------
-- 6) Ventas por prenda (reporte VENTAS)
-- ---------------------------------------------------------------------------
SELECT
    pr.nombre AS prenda,
    SUM(li.cantidad) AS unidades,
    SUM(li.subtotal) AS valor
FROM pedido_items li
JOIN pedidos p ON p.id = li.pedido_id
JOIN prendas pr ON pr.id = li.prenda_id
WHERE p.fecha_creacion >= :startDate
  AND p.fecha_creacion < DATE_ADD(:endDate, INTERVAL 1 DAY)
  AND p.estado IN ('PAGADO', 'REGISTRADO', 'EN_PREPARACION', 'PARCIALMENTE_ENVIADO', 'ENVIADO')
  AND (:categoria IS NULL OR pr.categoria = :categoria)
  AND (:vendedorId IS NULL OR pr.vendedor_id = :vendedorId)
GROUP BY pr.id, pr.nombre
ORDER BY unidades DESC;

-- ---------------------------------------------------------------------------
-- 7) Demanda por prenda y talla (reporte DEMANDA)
--    El reporte agrupa por talla, sin separar color: la demanda se mide por
--    talla. La línea del pedido conserva su color para auditoría.
-- ---------------------------------------------------------------------------
SELECT
    pr.nombre AS prenda,
    li.talla,
    SUM(li.cantidad) AS unidades,
    SUM(li.subtotal) AS valor
FROM pedido_items li
JOIN pedidos p ON p.id = li.pedido_id
JOIN prendas pr ON pr.id = li.prenda_id
WHERE p.fecha_creacion >= :startDate
  AND p.fecha_creacion < DATE_ADD(:endDate, INTERVAL 1 DAY)
  AND p.estado IN ('PAGADO', 'REGISTRADO', 'EN_PREPARACION', 'PARCIALMENTE_ENVIADO', 'ENVIADO')
  AND (:categoria IS NULL OR pr.categoria = :categoria)
  AND (:vendedorId IS NULL OR pr.vendedor_id = :vendedorId)
GROUP BY pr.id, pr.nombre, li.talla
ORDER BY unidades DESC, pr.nombre, li.talla;

-- ---------------------------------------------------------------------------
-- 8) Inventario: filas del reporte (reporte INVENTARIO)
--    Una fila por variante: prenda + talla + color. Sirve además como
--    verificación previa del límite de 5 000 filas.
-- ---------------------------------------------------------------------------
SELECT
    pr.nombre AS prenda,
    COALESCE(v.nombre_tienda, v.nombre) AS tienda,
    pr.categoria,
    t.talla,
    t.color,
    t.stock,
    t.stock_minimo,
    CASE
        WHEN t.stock_minimo IS NULL THEN 'Sin mínimo'
        WHEN t.stock = 0 THEN 'Agotado'
        WHEN t.stock <= t.stock_minimo THEN 'Stock bajo'
        ELSE 'Disponible'
    END AS nivel
FROM prenda_tallas t
JOIN prendas pr ON pr.id = t.prenda_id
JOIN usuarios v ON v.id = pr.vendedor_id
WHERE pr.estado = 'ACTIVA'
  AND (:categoria IS NULL OR pr.categoria = :categoria)
  AND (:vendedorId IS NULL OR v.id = :vendedorId)
ORDER BY v.id, pr.nombre, t.talla, t.color;

-- ---------------------------------------------------------------------------
-- 9) Inventario: cuántas filas tendría el reporte (debe ser <= 5000)
--    Cuenta variantes (prenda + talla + color), no tallas distintas.
-- ---------------------------------------------------------------------------
SELECT COUNT(*) AS filas_reporte
FROM prenda_tallas t
JOIN prendas pr ON pr.id = t.prenda_id
WHERE pr.estado = 'ACTIVA'
  AND (:categoria IS NULL OR pr.categoria = :categoria)
  AND (:vendedorId IS NULL OR pr.vendedor_id = :vendedorId);

-- ---------------------------------------------------------------------------
-- 10) Calidad del registro de búsquedas
--     - término normalizado vacío o demasiado largo
--     - categoría nula cuando el término no es genérico (solo informativo)
-- ---------------------------------------------------------------------------
SELECT
    SUM(CASE WHEN termino_normalizado IS NULL OR TRIM(termino_normalizado) = '' THEN 1 ELSE 0 END) AS terminos_vacios,
    SUM(CASE WHEN CHAR_LENGTH(termino_normalizado) > 200 THEN 1 ELSE 0 END) AS terminos_largos,
    SUM(CASE WHEN categoria IS NULL THEN 1 ELSE 0 END) AS busquedas_sin_categoria,
    COUNT(*) AS total
FROM busqueda_catalogo;

-- ---------------------------------------------------------------------------
-- 11) Términos que más crecen en la última semana frente a la anterior
--     Útil para detectar tendencias antes de que existan ventas.
-- ---------------------------------------------------------------------------
WITH semanas AS (
    SELECT
        termino_normalizado,
        MIN(termino_original) AS termino,
        SUM(CASE WHEN creado_en >= DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY) THEN 1 ELSE 0 END) AS ultima_semana,
        SUM(CASE WHEN creado_en >= DATE_SUB(CURRENT_DATE, INTERVAL 14 DAY)
                  AND creado_en <  DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY) THEN 1 ELSE 0 END) AS semana_previa
    FROM busqueda_catalogo
    GROUP BY termino_normalizado
)
SELECT
    termino,
    semana_previa,
    ultima_semana,
    ultima_semana - semana_previa AS variacion
FROM semanas
WHERE ultima_semana > 0
ORDER BY variacion DESC, ultima_semana DESC
LIMIT 20;
