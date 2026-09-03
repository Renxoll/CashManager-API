package pe.smartcash.cash.workspaces.domain.services;

import java.util.UUID;

public record WorkspaceCategoryDetail(
    UUID id, String code, String displayName, String icon, int position, boolean archived) {}
