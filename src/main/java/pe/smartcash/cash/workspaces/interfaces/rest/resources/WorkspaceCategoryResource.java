package pe.smartcash.cash.workspaces.interfaces.rest.resources;

import java.util.UUID;

public record WorkspaceCategoryResource(
    UUID id, String code, String displayName, String icon, int position, boolean archived) {}
