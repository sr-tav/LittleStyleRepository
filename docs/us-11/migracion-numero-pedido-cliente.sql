-- Migracion manual de MySQL 8: numero de pedido consecutivo por cliente (auditoria, punto 4.2).
-- Hacer respaldo y ejecutar una sola vez. Produccion usa ddl-auto=validate, asi que sin este script
-- la aplicacion no arranca. Requiere MySQL 8.0 o superior (usa ROW_NUMBER).
-- En desarrollo/local con ddl-auto=update Hibernate crea la columna, pero NO rellena los pedidos
-- existentes ni garantiza el indice unico sobre filas repetidas: ejecuta igualmente los pasos 2 y 3.

-- 1. Columna nueva. Se deja nula para poder rellenar los pedidos que ya existen.
ALTER TABLE pedidos ADD COLUMN numero_cliente INT NULL;

-- 2. Rellena los pedidos existentes: 1, 2, 3... por cliente, en orden de creacion (id ascendente).
UPDATE pedidos p
JOIN (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY cliente_id ORDER BY id) AS numero
    FROM pedidos
) t ON t.id = p.id
SET p.numero_cliente = t.numero;

-- 3. Un cliente no puede repetir consecutivo. Se crea despues del relleno para que no falle.
ALTER TABLE pedidos
    ADD UNIQUE INDEX uk_pedidos_cliente_numero (cliente_id, numero_cliente);

-- Verificaciones (ambas deben devolver 0 filas):
-- a) Pedidos sin consecutivo.
SELECT id, cliente_id FROM pedidos WHERE numero_cliente IS NULL;
-- b) Clientes cuya numeracion tiene huecos o repetidos (el maximo debe igualar la cantidad de pedidos).
SELECT cliente_id, COUNT(*) AS pedidos, MAX(numero_cliente) AS maximo
FROM pedidos
GROUP BY cliente_id
HAVING COUNT(*) <> MAX(numero_cliente);

-- Nota: el id de la tabla (pedidos.id) NO cambia. Sigue siendo el identificador interno que usan las
-- URLs (/cliente/pedidos/{id}), el vendedor, los despachos y el historial. Solo cambia el numero que el
-- cliente ve en pantalla y en los correos.
