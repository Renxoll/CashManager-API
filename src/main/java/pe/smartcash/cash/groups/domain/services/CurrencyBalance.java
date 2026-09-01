package pe.smartcash.cash.groups.domain.services;

import java.math.BigDecimal;

/** Saldo neto en una moneda puntual -- positivo = le deben, negativo = debe. Un grupo con
 * gastos en varias monedas tiene un {@code CurrencyBalance} por cada una, sin conversión. */
public record CurrencyBalance(String currency, BigDecimal amount) {}
