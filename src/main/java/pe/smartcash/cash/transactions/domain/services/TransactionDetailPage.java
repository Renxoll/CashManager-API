package pe.smartcash.cash.transactions.domain.services;

import java.util.List;

/** Read-model paginado devuelto por {@link TransactionQueryService}. */
public record TransactionDetailPage(List<TransactionDetail> items, int page, int size, long totalElements) {}
