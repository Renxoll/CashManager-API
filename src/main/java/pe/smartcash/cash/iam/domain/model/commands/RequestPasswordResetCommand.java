package pe.smartcash.cash.iam.domain.model.commands;

/**
 * El usuario pide un enlace para restablecer su contraseña. Se resuelve siempre igual
 * (sin error) exista o no una cuenta con ese email -- ver {@code IamCommandServiceImpl}.
 */
public record RequestPasswordResetCommand(String email) {}
