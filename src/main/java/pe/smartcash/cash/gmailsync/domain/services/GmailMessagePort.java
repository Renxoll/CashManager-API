package pe.smartcash.cash.gmailsync.domain.services;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Puerto hacia la API de Gmail; la implementación real (REST) vive en infrastructure.gmail. */
public interface GmailMessagePort {

  /**
   * Busca correos de {@code senderDomains} recibidos después de {@code since} (o de
   * cualquier antigüedad si {@code since} es {@code null} -- primera sincronización). La
   * sintaxis de búsqueda de Gmail ({@code from:(...) after:...}) es un detalle de la
   * implementación.
   */
  List<GmailMessage> findMatchingMessagesSince(String accessToken, Instant since, Set<String> senderDomains);
}
