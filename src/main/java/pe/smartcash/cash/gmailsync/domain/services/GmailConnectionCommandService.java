package pe.smartcash.cash.gmailsync.domain.services;

import pe.smartcash.cash.gmailsync.domain.model.commands.DisconnectGmailConnectionCommand;
import pe.smartcash.cash.gmailsync.domain.model.commands.StoreGmailConnectionCommand;

public interface GmailConnectionCommandService {

  /** Crea una conexión nueva si el (usuario, email) no existía, o rota los tokens en la
   * misma fila si ya existía (reautenticar la misma cuenta). Un usuario con varias cuentas
   * de Gmail tiene una fila por cada una. */
  void handle(StoreGmailConnectionCommand command);

  /** 404 (vía {@code GmailConnectionNotFoundException}) si no existe o no es del usuario. */
  void handle(DisconnectGmailConnectionCommand command);
}
