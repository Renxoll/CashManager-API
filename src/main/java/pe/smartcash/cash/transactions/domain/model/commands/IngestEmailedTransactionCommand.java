package pe.smartcash.cash.transactions.domain.model.commands;

/**
 * Ingesta vía SendGrid Inbound Parse: a diferencia de {@link IngestBankNotificationCommand},
 * el userId no lo trae el caller — hay que resolverlo a partir de {@code inboxAddress} (ver
 * {@code UserDirectory.findUserIdByInboxAddress}), y {@code fromAddress} habilita la política
 * de remitente confiable antes de aceptar el correo como una transacción real.
 */
public record IngestEmailedTransactionCommand(String inboxAddress, String fromAddress, String rawText) {}
