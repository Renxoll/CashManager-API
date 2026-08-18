package pe.smartcash.cash.iam.infrastructure.hashing;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pe.smartcash.cash.iam.domain.model.valueobjects.HashedPassword;
import pe.smartcash.cash.iam.domain.services.PasswordHasher;

@Component
class BCryptPasswordHasherAdapter implements PasswordHasher {

  private final PasswordEncoder encoder;

  BCryptPasswordHasherAdapter(PasswordEncoder encoder) {
    this.encoder = encoder;
  }

  @Override
  public HashedPassword hash(String rawPassword) {
    return new HashedPassword(encoder.encode(rawPassword));
  }

  @Override
  public boolean matches(String rawPassword, HashedPassword hashedPassword) {
    return encoder.matches(rawPassword, hashedPassword.value());
  }
}
