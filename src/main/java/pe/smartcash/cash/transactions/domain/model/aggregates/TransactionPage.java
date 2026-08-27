package pe.smartcash.cash.transactions.domain.model.aggregates;

import java.util.List;

/** Página de agregados devuelta por el puerto -- {@code Pageable}/{@code Page} de Spring Data no salen de infrastructure. */
public record TransactionPage(List<Transaction> items, long totalElements) {}
