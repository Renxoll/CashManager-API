-- Tabla que usa spring-modulith-events-jpa para el registro durable de eventos de dominio
-- (ver TransactionCommandServiceImpl.on(TransactionReceived), @ApplicationModuleListener):
-- el evento se registra acá en el mismo commit que persiste la transacción, así que si el
-- proceso muere antes de que el listener async corra, queda pendiente para reintento en vez
-- de perderse.
--
-- Se crea acá, vía Flyway, en vez de depender de
-- spring.modulith.events.jdbc-schema-initialization.enabled=true: esa property no está
-- creando la tabla en la práctica (probablemente por la interacción con Flyway ya
-- gestionando el schema), así que cualquier flujo que publique un evento de dominio
-- fallaba con 500 (relation "event_publication" does not exist) -- bug real encontrado
-- reproduciendo el flujo de ingesta por correo end-to-end, no por inspección de código.
-- DDL copiado tal cual del schema v2 que trae spring-modulith-events-jdbc:2.1.0
-- (org/springframework/modulith/events/jdbc/schemas/v2/schema-postgresql.sql).
CREATE TABLE IF NOT EXISTS event_publication
(
    id                     UUID                     NOT NULL,
    listener_id            TEXT                     NOT NULL,
    event_type             TEXT                     NOT NULL,
    serialized_event       TEXT                     NOT NULL,
    publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date        TIMESTAMP WITH TIME ZONE,
    status                 TEXT,
    completion_attempts    INT,
    last_resubmission_date TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS event_publication_serialized_event_hash_idx ON event_publication USING hash (serialized_event);
CREATE INDEX IF NOT EXISTS event_publication_by_completion_date_idx ON event_publication (completion_date);
