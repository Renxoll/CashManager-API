package pe.smartcash.cash;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Redis para todos los tests que levantan el contexto completo. Hace falta aunque el test no
 * use Redis: {@code RateLimiterConfig} abre la conexión de Bucket4j al arrancar, y sin Redis el
 * contexto entero falla. Se importa con {@code @Import(RedisTestConfiguration.class)}; el
 * {@code @ServiceConnection} registra host/puerto igual que spring-boot-docker-compose en dev.
 *
 * <p>Es un {@code @Bean} y no un campo estático con {@code @ImportTestcontainers} porque el
 * plugin de GraalVM corre {@code processTestAot} antes de los tests, y el procesamiento AOT no
 * soporta los contenedores registrados por esa anotación. Como bean, Spring lo arranca con el
 * contexto y lo reutiliza mientras ese contexto siga en la cache de tests.
 */
@TestConfiguration(proxyBeanMethods = false)
public class RedisTestConfiguration {

  @Bean
  @ServiceConnection(name = "redis")
  GenericContainer<?> redisContainer() {
    return new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
  }
}
