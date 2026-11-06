-- Migracion manual de MySQL: direccion estructurada (auditoria, punto 7).
-- Hacer respaldo y ejecutar una sola vez. Producción usa ddl-auto=validate.
-- Las columnas guardan texto cifrado (TextoLegadoCifradoConverter), igual que `direccion`.
ALTER TABLE direcciones_cliente
    ADD COLUMN tipo_via VARCHAR(512) NULL,
    ADD COLUMN numero_via VARCHAR(512) NULL,
    ADD COLUMN numero_cruce VARCHAR(512) NULL,
    ADD COLUMN placa VARCHAR(512) NULL;

-- Las direcciones guardadas antes de este cambio conservan `direccion` como texto libre y
-- las nuevas columnas en NULL: la aplicación pide completar la dirección estructurada la
-- próxima vez que el cliente entre al checkout o a «Mi cuenta». Los pedidos no cambian:
-- `pedidos.direccion` guarda el texto compuesto («Calle 45 # 12 - 34»).
