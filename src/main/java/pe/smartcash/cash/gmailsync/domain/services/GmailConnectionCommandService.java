package pe.smartcash.cash.gmailsync.domain.services;

import pe.smartcash.cash.gmailsync.domain.model.commands.StoreGmailConnectionCommand;

public interface GmailConnectionCommandService {

  /** Crea la conexión si es la primera vez, o reemplaza los tokens si el usuario ya
   * tenía una (reconectar tras revocar acceso, o simplemente repetir el flujo). */
  void handle(StoreGmailConnectionCommand command);
}
