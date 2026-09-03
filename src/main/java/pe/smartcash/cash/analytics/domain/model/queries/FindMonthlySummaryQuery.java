package pe.smartcash.cash.analytics.domain.model.queries;

import java.util.UUID;

/** {@code workspaceId} null = módulo "General" del usuario (el resumen por defecto). */
public record FindMonthlySummaryQuery(UUID userId, UUID workspaceId) {}
