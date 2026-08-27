package pe.smartcash.cash.transactions.domain.services;

import pe.smartcash.cash.transactions.domain.model.commands.ApprovePendingSenderCommand;
import pe.smartcash.cash.transactions.domain.model.commands.RejectPendingSenderCommand;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;

public interface PendingSenderCommandService {

  /**
   * Un correo de un remitente no confiable acaba de llegar. Si es la primera vez que se ve
   * ese dominio para este usuario, crea la fila PENDING; si ya existe y sigue PENDING, suma
   * una observación más; si ya fue decidida (aprobada o rechazada), no hace nada -- ver
   * {@code PendingSender.recordAnotherSighting}.
   */
  void recordSighting(UserId userId, String fromAddress, String rawText);

  /** Aprobar habilita el dominio en {@code UserTrustedSenderRepository} para correos futuros. */
  void handle(ApprovePendingSenderCommand command);

  void handle(RejectPendingSenderCommand command);
}
