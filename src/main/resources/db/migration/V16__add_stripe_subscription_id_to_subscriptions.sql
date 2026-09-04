-- Hasta ahora nada en el sistema guardaba a qué Subscription de Stripe corresponde una fila
-- de esta tabla: StripeWebhookController activaba la suscripción local a partir de la
-- metadata (userId/planCode) de la sesión de Checkout, pero nunca persistía el id de
-- suscripción que Stripe devuelve en esa misma sesión. Sin ese id, cancelar en la app no
-- tenía forma de cancelar también en Stripe -- el usuario quedaba CANCELED acá mientras
-- Stripe le seguía cobrando cada ciclo. NULL para FREE (nunca pasa por Stripe) y para las
-- suscripciones PREMIUM ya existentes antes de este cambio (no recuperable retroactivamente
-- desde acá; se resuelve manualmente en el dashboard de Stripe si hiciera falta).
ALTER TABLE subscriptions
    ADD COLUMN stripe_subscription_id VARCHAR(255);

CREATE UNIQUE INDEX idx_subscriptions_stripe_subscription_id ON subscriptions (stripe_subscription_id)
    WHERE stripe_subscription_id IS NOT NULL;
