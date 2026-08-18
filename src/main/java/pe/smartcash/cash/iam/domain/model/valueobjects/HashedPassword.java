package pe.smartcash.cash.iam.domain.model.valueobjects;

/** Nunca envuelve una contraseña en texto plano: solo el hash ya calculado por {@code PasswordHasher}. */
public record HashedPassword(String value) {

  public HashedPassword {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("El hash de contraseña no puede estar vacío");
    }
  }
}
