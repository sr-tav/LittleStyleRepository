-- Migración a despachos por vendedor (US-12 multi-vendedor).
-- Ejecutar una sola vez sobre bases existentes ANTES de desplegar el cambio.
-- En local (H2 en archivo con ddl-auto=update) basta borrar
-- data/littlestyle-local.mv.db y dejar que Hibernate regenere el esquema.
-- En MySQL/MariaDB (prod) ejecutar este script.

-- 1. Tabla de despachos: un paquete por pedido y vendedor.
CREATE TABLE IF NOT EXISTS despachos (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  pedido_id BIGINT NOT NULL,
  vendedor_id BIGINT NOT NULL,
  estado VARCHAR(30) NOT NULL,
  numero_guia VARCHAR(60),
  fecha_actualizacion DATETIME NOT NULL,
  UNIQUE KEY uk_despacho_pedido_vendedor (pedido_id, vendedor_id),
  FOREIGN KEY (pedido_id) REFERENCES pedidos(id),
  FOREIGN KEY (vendedor_id) REFERENCES usuarios(id)
);

-- 2. Vínculo línea → despacho e historial → despacho (nulables).
ALTER TABLE pedido_items
  ADD COLUMN IF NOT EXISTS despacho_id BIGINT NULL,
  ADD FOREIGN KEY IF NOT EXISTS (despacho_id) REFERENCES despachos(id);
ALTER TABLE pedido_estados
  ADD COLUMN IF NOT EXISTS despacho_id BIGINT NULL,
  ADD FOREIGN KEY IF NOT EXISTS (despacho_id) REFERENCES despachos(id);

-- 3. Backfill: un despacho por vendedor para pedidos ya existentes.
INSERT INTO despachos (pedido_id, vendedor_id, estado, numero_guia, fecha_actualizacion)
SELECT DISTINCT i.pedido_id, pr.vendedor_id, p.estado, p.numero_guia, p.fecha_actualizacion
FROM pedido_items i
JOIN prendas pr ON pr.id = i.prenda_id
JOIN pedidos p ON p.id = i.pedido_id
WHERE p.estado IN ('REGISTRADO','EN_PREPARACION','ENVIADO')
  AND NOT EXISTS (
    SELECT 1 FROM despachos d
    WHERE d.pedido_id = i.pedido_id AND d.vendedor_id = pr.vendedor_id
  );

UPDATE pedido_items i
JOIN prendas pr ON pr.id = i.prenda_id
JOIN despachos d ON d.pedido_id = i.pedido_id AND d.vendedor_id = pr.vendedor_id
SET i.despacho_id = d.id
WHERE i.despacho_id IS NULL;
