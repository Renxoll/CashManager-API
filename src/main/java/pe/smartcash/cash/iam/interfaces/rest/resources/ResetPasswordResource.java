package pe.smartcash.cash.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordResource(@NotBlank String token, @NotBlank @Size(min = 8) String password) {}
