package pe.smartcash.cash.advisor.domain.model.queries;

import java.util.UUID;

public record AskFinancialAdvisorQuery(UUID userId, String message) {}
