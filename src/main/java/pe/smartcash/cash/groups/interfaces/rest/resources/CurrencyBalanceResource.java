package pe.smartcash.cash.groups.interfaces.rest.resources;

import java.math.BigDecimal;

public record CurrencyBalanceResource(String currency, BigDecimal amount) {}
