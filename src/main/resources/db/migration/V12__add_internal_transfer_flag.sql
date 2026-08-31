-- Marca de "transferencia entre cuentas propias" (autodepósito): mover plata de una cuenta
-- propia a otra genera una notificación de gasto en una y de ingreso en otra, pero no es
-- consumo ni ingreso real -- inflaría el total del mes en ambos lados. Es un eje ortogonal
-- al type (un lado es EXPENSE, el otro INCOME) y al status, por eso una columna booleana
-- aparte y no un tercer valor de type. La detección automática es difícil sin conocer las
-- cuentas del usuario, así que arranca siempre en FALSE y el usuario la activa a mano desde
-- la app (analytics excluye estas filas de los totales y del desglose).
ALTER TABLE transactions
    ADD COLUMN internal_transfer BOOLEAN NOT NULL DEFAULT FALSE;
