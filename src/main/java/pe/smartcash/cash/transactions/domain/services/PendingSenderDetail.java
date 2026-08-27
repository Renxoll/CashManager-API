package pe.smartcash.cash.transactions.domain.services;

import java.time.Instant;
import pe.smartcash.cash.transactions.domain.model.valueobjects.PendingSenderId;

public record PendingSenderDetail(
    PendingSenderId id, String fromAddress, String domain, String sampleSnippet, int occurrenceCount, Instant firstSeenAt, Instant lastSeenAt) {}
