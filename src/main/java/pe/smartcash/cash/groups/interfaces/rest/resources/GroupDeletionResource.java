package pe.smartcash.cash.groups.interfaces.rest.resources;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Estado de la solicitud de borrado PENDING del grupo (null si no hay ninguna abierta). */
public record GroupDeletionResource(
    UUID requestedBy,
    String requestedByDisplayName,
    Instant requestedAt,
    List<UUID> approvedByUserIds,
    List<UUID> pendingApprovalFrom) {}
