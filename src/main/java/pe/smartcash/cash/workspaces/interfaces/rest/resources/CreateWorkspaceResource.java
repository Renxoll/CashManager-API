package pe.smartcash.cash.workspaces.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** {@code colorHex} e {@code icon} son opcionales -- si vienen null o vacíos el dominio pone
 * los de por defecto (#8B5CF6 / wallet). */
public record CreateWorkspaceResource(
    @NotBlank @Size(max = 60) String name,
    @Pattern(regexp = "^(#[0-9A-Fa-f]{6})?$", message = "colorHex debe tener formato #RRGGBB") String colorHex,
    @Size(max = 40) String icon) {}
