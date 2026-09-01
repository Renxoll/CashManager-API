package pe.smartcash.cash.groups.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record CreateGroupResource(@NotBlank String name) {}
