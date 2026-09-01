package pe.smartcash.cash.groups.domain.services;

import java.math.BigDecimal;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

/** Una transferencia sugerida por {@link DebtSimplifier} para saldar un grupo con el mínimo
 * número de pagos posible. No es un pago real hasta que alguien lo registra como {@code
 * Settlement}. */
public record SuggestedSettlement(UserId from, UserId to, BigDecimal amount) {}
