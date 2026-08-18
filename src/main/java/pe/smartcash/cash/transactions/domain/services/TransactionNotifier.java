package pe.smartcash.cash.transactions.domain.services;

public interface TransactionNotifier {

  void notify(NotificationTarget target, String message);
}
