package pe.smartcash.cash.gmailsync.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

public record GmailConnectionId(UUID value) {

  public GmailConnectionId {
    Objects.requireNonNull(value, "value");
  }

  public static GmailConnectionId newId() {
    return new GmailConnectionId(UUID.randomUUID());
  }

  public static GmailConnectionId of(UUID value) {
    return new GmailConnectionId(value);
  }
}
