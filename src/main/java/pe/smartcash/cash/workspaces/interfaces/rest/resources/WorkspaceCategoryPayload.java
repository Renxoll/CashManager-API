package pe.smartcash.cash.workspaces.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Alta y edición de una categoría de módulo. El {@code code} lo deriva el backend del
 * rótulo, no lo manda el cliente. */
public record WorkspaceCategoryPayload(
    @NotBlank @Size(max = 60) String displayName, @Size(max = 40) String icon) {}
