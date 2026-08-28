package pe.smartcash.cash.gmailsync.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * {@code @EnableScheduling} vive acá (scoped a este módulo, no en {@code CashApplication})
 * porque hoy es el único bounded context con un job programado ({@code GmailPollingJob}) --
 * si otro contexto necesita @Scheduled después, esta anotación es idempotente entre
 * configuraciones, así que no hay conflicto en tenerla declarada más de una vez.
 */
@Configuration
@EnableScheduling
class GmailSyncConfig {}
