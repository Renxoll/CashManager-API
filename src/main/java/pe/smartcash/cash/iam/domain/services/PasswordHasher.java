package pe.smartcash.cash.iam.domain.services;

import pe.smartcash.cash.iam.domain.model.valueobjects.HashedPassword;

/** Puerto: el dominio nunca sabe qué algoritmo hashea (BCrypt hoy, en infrastructure.hashing). */
public interface PasswordHasher {

  HashedPassword hash(String rawPassword);

  boolean matches(String rawPassword, HashedPassword hashedPassword);
}
