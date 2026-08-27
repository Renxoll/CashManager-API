package pe.smartcash.cash.gmailsync.domain.model.aggregates;

import java.util.List;
import java.util.Optional;
import pe.smartcash.cash.gmailsync.domain.model.valueobjects.UserId;

public interface GmailConnectionRepository {

  void save(GmailConnection connection);

  Optional<GmailConnection> findByUserId(UserId userId);

  /** Usado por el job programado: recorre todas las conexiones activas en cada corrida. */
  List<GmailConnection> findAll();
}
