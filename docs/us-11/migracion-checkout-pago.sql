-- Migracion manual de MySQL para el checkout que crea pedidos tras aprobar Wompi.
-- Hacer respaldo y ejecutar solo una vez sobre un esquema anterior a este cambio.
-- Antes de continuar, esta consulta debe devolver 0:
SELECT COUNT(*) AS transacciones_sin_cliente
FROM transacciones_pago tp
LEFT JOIN pedidos p ON p.id = tp.pedido_id
WHERE tp.pedido_id IS NULL OR p.cliente_id IS NULL;

-- Las transacciones historicas se enlazan al mismo cliente propietario del pedido.
ALTER TABLE transacciones_pago
    ADD COLUMN cliente_id BIGINT NULL,
    ADD COLUMN snapshot_checkout TEXT NULL,
    ADD COLUMN clave_idempotencia VARCHAR(64) NULL;

UPDATE transacciones_pago tp
JOIN pedidos p ON p.id = tp.pedido_id
SET tp.cliente_id = p.cliente_id
WHERE tp.cliente_id IS NULL;

ALTER TABLE transacciones_pago
    MODIFY COLUMN pedido_id BIGINT NULL,
    ADD INDEX idx_transaccion_pago_cliente (cliente_id),
    ADD UNIQUE INDEX uk_transaccion_cliente_idempotencia (cliente_id, clave_idempotencia),
    ADD CONSTRAINT fk_transaccion_pago_cliente
        FOREIGN KEY (cliente_id) REFERENCES usuarios (id);
