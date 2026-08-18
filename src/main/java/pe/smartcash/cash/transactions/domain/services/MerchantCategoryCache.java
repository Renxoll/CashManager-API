package pe.smartcash.cash.transactions.domain.services;

import java.util.Optional;
import pe.smartcash.cash.transactions.domain.model.valueobjects.CategoryCode;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Merchant;

public interface MerchantCategoryCache {

  Optional<CategoryCode> findCategoryFor(Merchant merchant);

  void remember(Merchant merchant, CategoryCode categoryCode);
}
