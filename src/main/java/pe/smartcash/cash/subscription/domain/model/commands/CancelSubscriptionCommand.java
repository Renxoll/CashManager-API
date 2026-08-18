package pe.smartcash.cash.subscription.domain.model.commands;

/** Cancela la suscripción ACTIVE del usuario — no se referencia por subscriptionId: el
 * dueño del request solo puede tener una activa a la vez, así que basta con el userId. */
public record CancelSubscriptionCommand(String userId) {}
