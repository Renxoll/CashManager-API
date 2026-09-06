package pe.smartcash.cash.iam.domain.model.commands;

/**
 * El usuario confirma el restablecimiento con el token crudo que llegó en el enlace del
 * correo y su contraseña nueva (en texto plano; se hashea en el caso de uso).
 */
public record ResetPasswordCommand(String rawToken, String newRawPassword) {}
