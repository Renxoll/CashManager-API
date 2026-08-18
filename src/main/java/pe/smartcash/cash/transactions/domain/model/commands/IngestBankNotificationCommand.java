package pe.smartcash.cash.transactions.domain.model.commands;

public record IngestBankNotificationCommand(String userId, String rawText) {}
