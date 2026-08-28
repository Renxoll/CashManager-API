package pe.smartcash.cash.transactions.application.internal.commandservices;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smartcash.cash.transactions.domain.exception.PendingSenderNotFoundException;
import pe.smartcash.cash.transactions.domain.model.aggregates.PendingSender;
import pe.smartcash.cash.transactions.domain.model.aggregates.PendingSenderRepository;
import pe.smartcash.cash.transactions.domain.model.aggregates.UserTrustedSenderRepository;
import pe.smartcash.cash.transactions.domain.model.commands.ApprovePendingSenderCommand;
import pe.smartcash.cash.transactions.domain.model.commands.RejectPendingSenderCommand;
import pe.smartcash.cash.transactions.domain.model.valueobjects.PendingSenderId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;
import pe.smartcash.cash.transactions.domain.policy.EmailDomainExtractor;
import pe.smartcash.cash.transactions.domain.services.PendingSenderCommandService;

@Service
class PendingSenderCommandServiceImpl implements PendingSenderCommandService {

  // Solo para mostrarle contexto al usuario en el approve/reject -- nunca se reingesta, así
  // que no hace falta el correo completo (a diferencia de Transaction.rawText, que sí necesita
  // el texto íntegro para que el LLM extraiga monto/comercio).
  private static final int SAMPLE_SNIPPET_MAX_LENGTH = 500;

  private final PendingSenderRepository pendingSenderRepository;
  private final UserTrustedSenderRepository userTrustedSenderRepository;
  private final Clock clock;

  PendingSenderCommandServiceImpl(
      PendingSenderRepository pendingSenderRepository, UserTrustedSenderRepository userTrustedSenderRepository, Clock clock) {
    this.pendingSenderRepository = pendingSenderRepository;
    this.userTrustedSenderRepository = userTrustedSenderRepository;
    this.clock = clock;
  }

  @Override
  @Transactional
  public void recordSighting(UserId userId, String fromAddress, String rawText) {
    String domain = EmailDomainExtractor.extract(fromAddress);
    if (domain == null) {
      return;
    }
    Instant now = clock.instant();
    Optional<PendingSender> existing = pendingSenderRepository.findByUserIdAndDomain(userId, domain);
    if (existing.isPresent()) {
      PendingSender pendingSender = existing.get();
      pendingSender.recordAnotherSighting(now);
      pendingSenderRepository.save(pendingSender);
      return;
    }
    PendingSender pendingSender =
        PendingSender.firstSighting(PendingSenderId.newId(), userId, fromAddress, domain, truncate(rawText), now);
    pendingSenderRepository.save(pendingSender);
  }

  @Override
  @Transactional
  public void handle(ApprovePendingSenderCommand command) {
    PendingSender pendingSender = requireOwned(command.pendingSenderId(), command.requestingUserId());
    Instant now = clock.instant();
    pendingSender.approve(now);
    pendingSenderRepository.save(pendingSender);
    userTrustedSenderRepository.trust(pendingSender.userId(), pendingSender.domain(), now);
  }

  @Override
  @Transactional
  public void handle(RejectPendingSenderCommand command) {
    PendingSender pendingSender = requireOwned(command.pendingSenderId(), command.requestingUserId());
    pendingSender.reject(clock.instant());
    pendingSenderRepository.save(pendingSender);
  }

  private PendingSender requireOwned(PendingSenderId id, UserId requestingUserId) {
    return pendingSenderRepository
        .findById(id)
        .filter(p -> p.userId().equals(requestingUserId))
        .orElseThrow(() -> new PendingSenderNotFoundException(id));
  }

  private String truncate(String rawText) {
    if (rawText == null) {
      return null;
    }
    return rawText.length() > SAMPLE_SNIPPET_MAX_LENGTH ? rawText.substring(0, SAMPLE_SNIPPET_MAX_LENGTH) : rawText;
  }
}
