package pe.smartcash.cash.groups.domain.services;

import java.time.Instant;
import java.util.List;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

/** Estado de la solicitud de borrado PENDING de un grupo, para el detalle del grupo. */
public record GroupDeletionDetail(
    UserId requestedBy,
    String requestedByDisplayName,
    Instant requestedAt,
    List<UserId> approvedByUserIds,
    List<UserId> pendingApprovalFrom) {}
