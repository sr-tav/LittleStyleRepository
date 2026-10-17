-- Consultas de evidencia para las métricas del Plan de Gestión de la Calidad (3.3 y 3.4, procesos 1 y 2).
-- Ver docs/calidad/evidencias-metricas-sprint2.md. Solo lectura: no modifican datos.

-- [P1-A] Tasa de adopción de recomendación de tallas (meta > 80 %)
SELECT COUNT(*) AS lineas_con_sugerencia, SUM(i.talla = i.talla_sugerida) AS con_talla_sugerida, ROUND(100 * SUM(i.talla = i.talla_sugerida) / COUNT(*), 1) AS porcentaje FROM pedido_items i JOIN pedidos p ON p.id = i.pedido_id WHERE i.talla_sugerida IS NOT NULL AND p.estado NOT IN ('PENDIENTE_PAGO', 'CANCELADO');
-- [P1-B] Completitud del perfil infantil (meta > 90 %). Completo = medición + colores + estampados
SELECT COUNT(*) AS perfiles, SUM(EXISTS (SELECT 1 FROM mediciones_crecimiento m WHERE m.perfil_id = p.id) AND EXISTS (SELECT 1 FROM perfil_colores_preferidos c WHERE c.perfil_id = p.id) AND EXISTS (SELECT 1 FROM perfil_estampados_preferidos e WHERE e.perfil_id = p.id)) AS completos, ROUND(100 * SUM(EXISTS (SELECT 1 FROM mediciones_crecimiento m WHERE m.perfil_id = p.id) AND EXISTS (SELECT 1 FROM perfil_colores_preferidos c WHERE c.perfil_id = p.id) AND EXISTS (SELECT 1 FROM perfil_estampados_preferidos e WHERE e.perfil_id = p.id)) / COUNT(*), 1) AS porcentaje FROM perfiles_infantiles p;
-- [P1-C] Frecuencia de actualización de perfiles (meta > 60 %)
SELECT COUNT(*) AS perfiles, SUM(fecha_actualizacion >= NOW() - INTERVAL 6 MONTH) AS actualizados_6_meses, ROUND(100 * SUM(fecha_actualizacion >= NOW() - INTERVAL 6 MONTH) / COUNT(*), 1) AS porcentaje FROM perfiles_infantiles;
-- [P1-D] Protección de datos sensibles (meta > 96 %): los valores deben verse ilegibles
SELECT id, LEFT(nombre, 30) AS nombre, LEFT(fecha_nacimiento, 30) AS fecha_nacimiento, LEFT(contextura, 30) AS contextura, LEFT(alergias_cifradas, 30) AS alergias FROM perfiles_infantiles LIMIT 3;
SELECT id, perfil_id, LEFT(fecha_medicion, 30) AS fecha, LEFT(estatura_cifrada, 30) AS estatura, LEFT(peso_cifrado, 30) AS peso FROM mediciones_crecimiento LIMIT 3;
-- [P2-A] Tasa de abandono del carrito (meta < 30 %) = (carritos_sin_comprar + checkouts_no_pagados) / (las tres columnas sumadas)
SELECT (SELECT COUNT(DISTINCT carrito_id) FROM carrito_items) AS carritos_sin_comprar, (SELECT COUNT(*) FROM pedidos WHERE estado IN ('PENDIENTE_PAGO', 'CANCELADO')) AS checkouts_no_pagados, (SELECT COUNT(*) FROM pedidos WHERE estado NOT IN ('PENDIENTE_PAGO', 'CANCELADO')) AS compras_pagadas;
-- [P2-B] Tiempo de sincronización de stock (meta < 500 ms): pago aprobado -> stock descontado
SELECT p.id AS pedido, ROUND(ABS(TIMESTAMPDIFF(MICROSECOND, t.fecha_actualizacion, h.fecha_cambio)) / 1000, 1) AS ms FROM transacciones_pago t JOIN pedidos p ON p.id = t.pedido_id JOIN pedido_estados h ON h.pedido_id = p.id AND h.estado_nuevo = 'PAGADO' WHERE t.estado = 'APROBADA' ORDER BY p.id DESC LIMIT 20;
-- [P2-C] Trazabilidad de estados (meta > 98 %) = correctos / pedidos_pagados
SELECT COUNT(*) AS pedidos_pagados, SUM(EXISTS (SELECT 1 FROM pedido_estados h WHERE h.pedido_id = p.id AND h.estado_nuevo = 'PAGADO') AND (p.estado NOT IN ('ENVIADO', 'PARCIALMENTE_ENVIADO') OR EXISTS (SELECT 1 FROM despachos d WHERE d.pedido_id = p.id AND d.numero_guia IS NOT NULL))) AS correctos FROM pedidos p WHERE p.estado NOT IN ('PENDIENTE_PAGO', 'CANCELADO');
-- [P2-D] Precisión de alertas de reabastecimiento (meta > 99 %) = con_alerta_previa / tallas_agotadas
SELECT COUNT(*) AS tallas_agotadas, SUM(EXISTS (SELECT 1 FROM alertas_reabastecimiento a WHERE a.prenda_id = t.prenda_id AND a.talla = t.talla)) AS con_alerta_previa FROM prenda_tallas t WHERE t.stock = 0 AND t.stock_minimo IS NOT NULL;
-- [P2-E] Pagos: estado de la transacción vs. estado del pedido (webhooks y preservación ante fallos)
SELECT t.estado AS transaccion, COALESCE(p.estado, '(sin pedido)') AS pedido, COUNT(*) AS cantidad FROM transacciones_pago t LEFT JOIN pedidos p ON p.id = t.pedido_id GROUP BY t.estado, p.estado ORDER BY t.estado;
-- [P2-F] Total de imágenes del catálogo (denominador de disponibilidad multimedia)
SELECT COUNT(*) AS imagenes FROM prenda_imagenes;
