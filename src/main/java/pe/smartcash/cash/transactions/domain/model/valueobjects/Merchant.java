package pe.smartcash.cash.transactions.domain.model.valueobjects;

public record Merchant(String name) {

  public Merchant {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("El comercio no puede estar vacío");
    }
    name = name.trim();
  }
}
