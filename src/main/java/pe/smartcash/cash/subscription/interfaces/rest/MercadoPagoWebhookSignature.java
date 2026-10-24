package pe.smartcash.cash.subscription.interfaces.rest;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Valida la cabecera {@code x-signature} (formato {@code ts=<epoch>,v1=<hmac>}) de las
 * notificaciones de Mercado Pago: HMAC-SHA256, con la clave secreta del panel, sobre el manifiesto
 * {@code id:<data.id>;request-id:<x-request-id>;ts:<ts>;}. Las partes ausentes se omiten.
 */
final class MercadoPagoWebhookSignature {

  private MercadoPagoWebhookSignature() {}

  static boolean isValid(String secret, String signatureHeader, String requestId, String dataId) {
    if (secret == null || secret.isBlank() || signatureHeader == null) {
      return false;
    }
    String ts = null;
    String v1 = null;
    for (String part : signatureHeader.split(",")) {
      String[] keyValue = part.trim().split("=", 2);
      if (keyValue.length != 2) {
        continue;
      }
      if (keyValue[0].equals("ts")) {
        ts = keyValue[1];
      } else if (keyValue[0].equals("v1")) {
        v1 = keyValue[1];
      }
    }
    if (ts == null || v1 == null) {
      return false;
    }
    StringBuilder manifest = new StringBuilder();
    if (dataId != null) {
      // Mercado Pago firma el id en minúsculas cuando es alfanumérico.
      manifest.append("id:").append(dataId.toLowerCase()).append(';');
    }
    if (requestId != null) {
      manifest.append("request-id:").append(requestId).append(';');
    }
    manifest.append("ts:").append(ts).append(';');
    String expected = hmacSha256Hex(secret, manifest.toString());
    // Comparación en tiempo constante.
    return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), v1.toLowerCase().getBytes(StandardCharsets.UTF_8));
  }

  static String hmacSha256Hex(String secret, String message) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("HmacSHA256 no disponible", e);
    }
  }
}
