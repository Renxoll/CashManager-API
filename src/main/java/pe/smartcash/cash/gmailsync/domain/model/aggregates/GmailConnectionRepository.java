package pe.smartcash.cash.gmailsync.domain.model.aggregates;

import java.util.List;
import java.util.Optional;
import pe.smartcash.cash.gmailsync.domain.model.valueobjects.GmailConnectionId;
import pe.smartcash.cash.gmailsync.domain.model.valueobjects.UserId;

public interface GmailConnectionRepository {

  /** Insert-or-update puro por {@code connection.id()} -- ya no sniffea por userId, el
   * caller decide explícitamente si es una conexión nueva o una a reconectar (ver
   * {@code GmailConnectionCommandServiceImpl}). */
  void save(GmailConnection connection);

  Optional<GmailConnection> findById(GmailConnectionId id);

  /** Un usuario puede tener varias cuentas de Gmail conectadas. */
  List<GmailConnection> findAllByUserId(UserId userId);

  /** Para decidir si reconectar (misma cuenta) o agregar una nueva (cuenta distinta). */
  Optional<GmailConnection> findByUserIdAndEmail(UserId userId, String email);

  void delete(GmailConnectionId id);

  /** Usado por el job programado: recorre todas las conexiones activas en cada corrida. */
  List<GmailConnection> findAll();
}
