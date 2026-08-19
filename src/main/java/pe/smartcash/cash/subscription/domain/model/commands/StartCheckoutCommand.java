package pe.smartcash.cash.subscription.domain.model.commands;

/** El usuario pide suscribirse a un plan pago: arranca un checkout, no activa nada todavía. */
public record StartCheckoutCommand(String userId, String planCode) {}
