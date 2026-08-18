package pe.smartcash.cash.profile.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record RegisterUserProfileResource(@NotBlank String displayName) {}
