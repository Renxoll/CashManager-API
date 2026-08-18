package pe.smartcash.cash.subscription.domain.services;

import pe.smartcash.cash.subscription.domain.model.commands.CancelSubscriptionCommand;
import pe.smartcash.cash.subscription.domain.model.commands.SubscribeCommand;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionId;

public interface SubscriptionCommandService {

  SubscriptionId handle(SubscribeCommand command);

  void handle(CancelSubscriptionCommand command);
}
