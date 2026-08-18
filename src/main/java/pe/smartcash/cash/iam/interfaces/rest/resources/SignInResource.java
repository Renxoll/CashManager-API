package pe.smartcash.cash.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record SignInResource(@NotBlank String email, @NotBlank String password) {}
