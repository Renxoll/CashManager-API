package pe.smartcash.cash.gmailsync.domain.services;

/** Vista de solo lectura de un mensaje de Gmail ya resuelto a texto plano -- el parsing de
 * MIME/multipart es un detalle de infraestructura, este contexto solo ve remitente + texto,
 * igual que {@code IngestBankNotificationCommand} del lado de Transactions. */
public record GmailMessage(String from, String rawText) {}
