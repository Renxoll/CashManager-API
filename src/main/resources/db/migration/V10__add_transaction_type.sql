-- Dirección del dinero (gasto vs. ingreso), eje ortogonal a la categoría: una transacción
-- INCOME no tiene categoría (category_id ya era nullable) porque hay un solo bucket
-- "Ingreso" en v1, sin subcategorías. Default EXPENSE para las filas existentes -- toda
-- transacción de hoy es un gasto por definición (la app nunca detectó ingresos antes).
ALTER TABLE transactions
    ADD COLUMN type VARCHAR(10) NOT NULL DEFAULT 'EXPENSE'
        CHECK (type IN ('EXPENSE', 'INCOME'));
