package pe.smartcash.cash.profile.domain.services;

import java.time.Instant;
import pe.smartcash.cash.profile.domain.model.valueobjects.UserId;

public record UserProfileDetail(UserId userId, String displayName, String fcmToken, Instant createdAt, Instant updatedAt) {}
