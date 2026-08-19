package pe.smartcash.cash.subscription.domain.services;

/** Resultado de iniciar un checkout: la URL a la que el cliente debe redirigir al usuario para pagar. */
public record CheckoutSession(String checkoutUrl) {}
