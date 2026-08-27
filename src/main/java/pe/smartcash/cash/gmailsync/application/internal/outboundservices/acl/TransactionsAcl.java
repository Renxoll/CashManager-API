package pe.smartcash.cash.gmailsync.application.internal.outboundservices.acl;

import org.springframework.stereotype.Component;
import pe.smartcash.cash.gmailsync.domain.services.BankNotificationIngestionPort;
import pe.smartcash.cash.transactions.domain.model.commands.IngestBankNotificationCommand;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;
import pe.smartcash.cash.transactions.domain.policy.TrustedBankSenderPolicy;
import pe.smartcash.cash.transactions.domain.services.PendingSenderCommandService;
import pe.smartcash.cash.transactions.domain.services.TransactionCommandService;

/**
 * Anti-Corruption Layer hacia Transactions: único punto donde GmailSync le habla a ese
 * contexto, y solo a través de su API pública de dominio ({@link TransactionCommandService},
 * {@link TrustedBankSenderPolicy}, {@link PendingSenderCommandService}), nunca importando
 * {@code transactions.domain.model.aggregates.*} ni otros internos. Vive en application (no
 * infrastructure): es orquestación entre contextos en el mismo proceso, no I/O técnico real
 * -- mismo criterio que {@code UserDirectoryAdapter} en Transactions.
 */
@Component
class TransactionsAcl implements BankNotificationIngestionPort {

  private final TransactionCommandService transactionCommandService;
  private final TrustedBankSenderPolicy trustedBankSenderPolicy;
  private final PendingSenderCommandService pendingSenderCommandService;

  TransactionsAcl(
      TransactionCommandService transactionCommandService,
      TrustedBankSenderPolicy trustedBankSenderPolicy,
      PendingSenderCommandService pendingSenderCommandService) {
    this.transactionCommandService = transactionCommandService;
    this.trustedBankSenderPolicy = trustedBankSenderPolicy;
    this.pendingSenderCommandService = pendingSenderCommandService;
  }

  @Override
  public void ingest(String userId, String rawText) {
    transactionCommandService.handle(new IngestBankNotificationCommand(userId, rawText));
  }

  @Override
  public boolean isTrustedSender(String userId, String fromAddress) {
    return trustedBankSenderPolicy.isSatisfiedBy(UserId.parse(userId), fromAddress);
  }

  @Override
  public void recordPendingSender(String userId, String fromAddress, String rawText) {
    pendingSenderCommandService.recordSighting(UserId.parse(userId), fromAddress, rawText);
  }
}
