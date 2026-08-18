package pe.smartcash.cash.transactions.domain.policy;

import java.math.RoundingMode;
import java.util.Locale;
import pe.smartcash.cash.transactions.domain.model.events.TransactionCategorized;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Money;
import pe.smartcash.cash.transactions.domain.services.CategoryCatalog;
import pe.smartcash.cash.transactions.domain.services.NotificationTarget;
import pe.smartcash.cash.transactions.domain.services.TransactionNotifier;
import pe.smartcash.cash.transactions.domain.services.UserDirectory;

/**
 * Solo compone otros puertos del dominio (nada de Spring/JPA/HTTP acá), por eso puede vivir
 * en domain igual que su contrato: es orquestación de negocio, no infraestructura.
 */
public class DefaultCategorizedExpenseNotificationPolicy implements CategorizedExpenseNotificationPolicy {

  private final UserDirectory userDirectory;
  private final TransactionNotifier notifier;
  private final CategoryCatalog categoryCatalog;

  public DefaultCategorizedExpenseNotificationPolicy(
      UserDirectory userDirectory, TransactionNotifier notifier, CategoryCatalog categoryCatalog) {
    this.userDirectory = userDirectory;
    this.notifier = notifier;
    this.categoryCatalog = categoryCatalog;
  }

  @Override
  public void enforce(TransactionCategorized event) {
    NotificationTarget target = userDirectory.findNotificationTarget(event.userId()).orElse(null);
    if (target == null) {
      return; // sin destino válido (sin token FCM), no hay nada que notificar
    }
    String categoryLabel = categoryCatalog.describe(event.categoryCode()).displayName();
    String message =
        "Gasto de %s en %s registrado en %s".formatted(formatMoney(event.money()), event.merchant().name(), categoryLabel);
    notifier.notify(target, message);
  }

  private String formatMoney(Money money) {
    String symbol = "PEN".equals(money.currency()) ? "S/" : money.currency().toUpperCase(Locale.ROOT) + " ";
    return symbol + money.amount().setScale(2, RoundingMode.HALF_UP);
  }
}
