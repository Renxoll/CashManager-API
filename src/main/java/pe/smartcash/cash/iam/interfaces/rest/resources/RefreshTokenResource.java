package pe.smartcash.cash.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenResource(@NotBlank String refreshToken) {}
