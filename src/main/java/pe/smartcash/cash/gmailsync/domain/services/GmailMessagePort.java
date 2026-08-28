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

  /**
   * Busca correos que PODRÍAN ser notificaciones de banco/billetera pero NO vienen de un
   * dominio ya confiable ({@code excludeDomains}) -- candidatos a remitente pendiente (ver
   * {@code PendingSender}). Es un heurístico por palabras clave transaccionales, no una
   * garantía: puede traer falsos positivos, pero eso es aceptable porque el resultado nunca
   * se ingesta directo, solo se encola para que el usuario decida.
   */
  List<GmailMessage> findCandidateMessagesSince(String accessToken, Instant since, Set<String> excludeDomains);
}
