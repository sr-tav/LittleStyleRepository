-- Migracion manual de MySQL para la seleccion de estampado por el cliente (auditoria, punto 8).
-- Hacer respaldo y ejecutar solo una vez sobre un esquema anterior a este cambio.
-- En desarrollo/local, con ddl-auto=update, Hibernate crea la tabla y las columnas nuevas; lo unico que
-- NO hace solo es cambiar el indice unico del carrito (paso 3).

-- 1. Estampados que ofrece cada prenda (el vendedor puede declarar varios).
CREATE TABLE IF NOT EXISTS prenda_estampados (
    prenda_id BIGINT NOT NULL,
    estampado VARCHAR(20) NOT NULL,
    PRIMARY KEY (prenda_id, estampado),
    CONSTRAINT fk_prenda_estampados_prenda FOREIGN KEY (prenda_id) REFERENCES prendas (id)
);

-- Las prendas existentes conservan su estampado unico: se copia a la nueva tabla.
INSERT IGNORE INTO prenda_estampados (prenda_id, estampado)
SELECT id, estampado FROM prendas WHERE estampado IS NOT NULL;

-- 2. Estampado elegido en cada linea del carrito y del pedido (nulo en lo anterior).
ALTER TABLE carrito_items ADD COLUMN estampado VARCHAR(20) NULL;
ALTER TABLE pedido_items ADD COLUMN estampado VARCHAR(20) NULL;

-- 3. La misma prenda, talla y color puede estar dos veces en el carrito con estampados distintos.
--    Se crea primero el indice nuevo y despues se retira el viejo (la clave foranea de carrito_id
--    necesita siempre un indice que empiece por esa columna).
ALTER TABLE carrito_items
    ADD UNIQUE INDEX uk_carrito_item_color_nuevo (carrito_id, prenda_id, talla, color, estampado);
ALTER TABLE carrito_items DROP INDEX uk_carrito_item_color;
ALTER TABLE carrito_items RENAME INDEX uk_carrito_item_color_nuevo TO uk_carrito_item_color;

-- Verificacion: debe devolver una fila por prenda con estampado y 0 prendas sin estampado disponible.
SELECT COUNT(*) AS prendas_sin_estampado
FROM prendas p
LEFT JOIN prenda_estampados e ON e.prenda_id = p.id
WHERE e.prenda_id IS NULL;
