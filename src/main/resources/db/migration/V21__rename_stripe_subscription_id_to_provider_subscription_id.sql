-- La pasarela de pagos pasa de Stripe a Culqi (Stripe no permite activar cuentas de empresas
-- en Perú). La columna guarda el id de la suscripción recurrente en el proveedor -- en Culqi,
-- sxn_live_... / sxn_test_... -- así que se renombra a algo neutral en vez de crear otra.
-- Las filas existentes con ids de Stripe (solo modo de prueba: la cuenta nunca se activó)
-- quedan como históricas: ningún webhook de Culqi va a coincidir con ellas.
ALTER TABLE subscriptions RENAME COLUMN stripe_subscription_id TO provider_subscription_id;

ALTER INDEX idx_subscriptions_stripe_subscription_id RENAME TO idx_subscriptions_provider_subscription_id;
