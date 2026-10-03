package pe.smartcash.cash;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Smoke test: el contexto completo levanta con la configuración real (Flyway corre todas las
 * migraciones contra Postgres, Redis conecta). Es el que avisa si una migración nueva o un bean
 * mal cableado rompe el arranque, antes que cualquier IT de un contexto en particular.
 */
@SpringBootTest
@Testcontainers
@Import(RedisTestConfiguration.class)
class CashApplicationTests {

  @Container
  @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

  @Test
  void contextLoads() {
  }

}
