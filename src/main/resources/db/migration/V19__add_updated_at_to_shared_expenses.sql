-- Permitir corregir un gasto compartido ya registrado (typo en la descripción, monto mal
-- tipeado, pagador o participantes equivocados -- ver SharedExpense.revise). Los shares se
-- recalculan y se reemplazan en el mismo save; acá solo agregamos la marca de "editado".
ALTER TABLE shared_expenses
    ADD COLUMN updated_at TIMESTAMPTZ;
