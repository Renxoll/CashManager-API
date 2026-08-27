package pe.smartcash.cash.transactions.domain.policy;

import java.util.Locale;

/**
 * "From:" de un correo -> dominio del remitente en minúsculas, o {@code null} si no se puede
 * extraer. Extraído de {@link AllowlistedBankSenderPolicy} porque {@code PendingSender}
 * necesita exactamente el mismo parseo para decidir a qué dominio le suma una nueva
 * observación -- que ambos coincidan importa: si el parseo de uno y otro divergiera, un
 * remitente podría marcarse pendiente pese a ya estar en el allowlist (o viceversa).
 */
public final class EmailDomainExtractor {

  private EmailDomainExtractor() {}

  public static String extract(String fromAddress) {
    if (fromAddress == null || fromAddress.isBlank()) {
      return null;
    }
    int at = fromAddress.lastIndexOf('@');
    if (at < 0 || at == fromAddress.length() - 1) {
      return null;
    }
    // El "From" de un correo puede venir como "Notificaciones BCP <alertas@bcp.com.pe>";
    // el dominio real está entre el último '@' y el primer delimitador de cierre que siga.
    String afterAt = fromAddress.substring(at + 1);
    int cut = afterAt.indexOf('>');
    if (cut >= 0) {
      afterAt = afterAt.substring(0, cut);
    }
    return afterAt.trim().toLowerCase(Locale.ROOT);
  }
}
