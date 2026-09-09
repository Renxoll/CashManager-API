package pe.smartcash.cash.groups.domain.model.valueobjects;

public enum DeletionRequestStatus {
  /** Abierta, juntando aprobaciones. */
  PENDING,
  /** Todos los miembros aceptados aprobaron y el grupo ya se borró. */
  COMPLETED,
  /** Algún miembro dio marcha atrás antes de que fuera unánime. */
  CANCELLED
}
