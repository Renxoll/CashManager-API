package pe.smartcash.cash.iam.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RequestPasswordResetResource(@NotBlank @Email String email) {}
