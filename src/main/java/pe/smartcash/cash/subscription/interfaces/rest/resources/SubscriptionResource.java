package pe.smartcash.cash.subscription.interfaces.rest.resources;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionResource(
    UUID subscriptionId, UUID userId, String planCode, String status, Instant startedAt, Instant renewsAt, Instant canceledAt) {}
