-- US-07: talla sugerida del motor en carrito y pedido (métrica RNF-11).
-- En dev/local y migraciones Hibernate aplica ddl-auto=update solo;
-- en prod (ddl-auto=validate) aplicar este script manualmente.
ALTER TABLE carrito_items ADD COLUMN talla_sugerida VARCHAR(10) NULL;
ALTER TABLE pedido_items ADD COLUMN talla_sugerida VARCHAR(10) NULL;
