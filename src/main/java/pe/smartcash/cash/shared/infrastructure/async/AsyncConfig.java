package pe.smartcash.cash.shared.infrastructure.async;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Pool dedicado para trabajo async de toda la aplicación — hoy solo lo usa el worker de
 * categorización ({@code TransactionCommandServiceImpl.on(TransactionReceived)}) vía
 * {@code @ApplicationModuleListener}, que internamente es {@code @Async}. El bean se llama
 * {@code applicationTaskExecutor} a propósito: es el nombre que Spring busca por convención
 * para {@code @Async} sin executor explícito, así que cualquier otro {@code @Async} futuro en
 * el proyecto cae acá solo, sin tener que nombrarlo en cada sitio de uso.
 */
@Configuration
@EnableAsync
class AsyncConfig implements AsyncConfigurer {

  @Override
  @Bean(name = "applicationTaskExecutor")
  public Executor getAsyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(16);
    executor.setQueueCapacity(200);
    executor.setThreadNamePrefix("async-worker-");
    // Si el pool y la cola están saturados (pico de webhooks bancarios), procesar en el hilo
    // que publica el evento en vez de tumbarlo con un RejectedExecutionException: el
    // publisher ya terminó su transacción (AFTER_COMMIT), así que ese hilo no es el del
    // request HTTP y bloquearlo un momento no afecta la latencia percibida por el cliente.
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
  }

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return new SimpleAsyncUncaughtExceptionHandler();
  }
}
