package pe.smartcash.cash.transactions.interfaces.rest.resources;

import java.util.List;

public record TransactionPageResource(List<TransactionResource> items, int page, int size, long totalElements) {}
