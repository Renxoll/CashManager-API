package pe.smartcash.cash.advisor.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record ChatMessageRequest(@NotBlank String message) {}
