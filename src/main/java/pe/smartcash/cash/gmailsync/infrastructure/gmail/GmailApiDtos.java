package pe.smartcash.cash.gmailsync.infrastructure.gmail;

import java.util.List;

/** DTOs de cable de la API de Gmail (v1, {@code users.messages.list} / {@code .get}). Solo
 * se modela lo que realmente se usa -- la respuesta real trae muchos más campos. */
record GmailMessageListResponse(List<GmailMessageRef> messages) {}

record GmailMessageRef(String id) {}

record GmailFullMessage(GmailMessagePayload payload) {}

record GmailMessagePayload(String mimeType, List<GmailHeader> headers, GmailMessageBody body, List<GmailMessagePayload> parts) {}

record GmailHeader(String name, String value) {}

record GmailMessageBody(String data) {}
