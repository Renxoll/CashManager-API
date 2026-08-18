package pe.smartcash.cash.iam.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpResource(
    @NotBlank @Email String email, @NotBlank @Size(min = 8) String password, @NotBlank String displayName) {}
