package pe.smartcash.cash.profile.interfaces.rest.resources;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResource(UUID userId, String displayName, String fcmToken, Instant createdAt, Instant updatedAt) {}
