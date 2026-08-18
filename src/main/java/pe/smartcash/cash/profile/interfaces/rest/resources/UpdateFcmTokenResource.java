package pe.smartcash.cash.profile.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record UpdateFcmTokenResource(@NotBlank String fcmToken) {}
