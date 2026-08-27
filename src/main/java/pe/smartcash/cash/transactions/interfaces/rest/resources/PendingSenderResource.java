package pe.smartcash.cash.transactions.interfaces.rest.resources;

import java.time.Instant;
import java.util.UUID;

public record PendingSenderResource(
    UUID pendingSenderId, String fromAddress, String domain, String sampleSnippet, int occurrenceCount, Instant firstSeenAt, Instant lastSeenAt) {}
