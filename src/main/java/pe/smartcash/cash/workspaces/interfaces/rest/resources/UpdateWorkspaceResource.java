package pe.smartcash.cash.workspaces.interfaces.rest.resources;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** PATCH parcial: cada campo null significa "no cambiar". */
public record UpdateWorkspaceResource(
    @Size(max = 60) String name,
    @Pattern(regexp = "^(#[0-9A-Fa-f]{6})?$", message = "colorHex debe tener formato #RRGGBB") String colorHex,
    @Size(max = 40) String icon) {}
