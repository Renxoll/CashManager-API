package pe.smartcash.cash.subscription.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record SubscribeResource(@NotBlank String planCode) {}
