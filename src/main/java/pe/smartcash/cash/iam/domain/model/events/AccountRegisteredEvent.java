package pe.smartcash.cash.iam.domain.model.events;

import java.util.UUID;

/**
 * Hecho de negocio: se registraron credenciales nuevas. Es un POJO puro (no extiende
 * {@code ApplicationEvent}) para que el dominio siga sin depender de Spring — se publica vía
 * {@code ApplicationEventPublisher#publishEvent(Object)}, que acepta cualquier objeto desde
 * Spring 4.2. Otros bounded contexts (p. ej. Profile) lo escuchan para orquestar el
 * onboarding sin que IAM conozca ni le importe quién reacciona a este evento.
 */
public record AccountRegisteredEvent(UUID userId, String email, String displayName) {}
