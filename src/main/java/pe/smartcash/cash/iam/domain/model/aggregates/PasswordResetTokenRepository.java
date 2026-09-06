package pe.smartcash.cash.iam.domain.model.aggregates;

import java.util.Optional;
import pe.smartcash.cash.iam.domain.model.valueobjects.UserId;

public interface PasswordResetTokenRepository {

  void save(PasswordResetToken token);

  Optional<PasswordResetToken> findByTokenHash(String tokenHash);

  /** Al pedir un enlace nuevo se borran los anteriores del usuario: solo el último queda vivo. */
  void deleteAllByUserId(UserId userId);
}
