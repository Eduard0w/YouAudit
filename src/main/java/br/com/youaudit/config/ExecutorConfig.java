package br.com.youaudit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Pool pequeno de threads para o trabalho em segundo plano (CONTRATOS.md §4: 2 a 4 threads).
 * Hoje só o lote de links usa; o pipeline assíncrono vai usar o mesmo pool.
 */
@Configuration
public class ExecutorConfig {

    @Bean
    public ThreadPoolTaskExecutor executorAuditoria() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("auditoria-");
        executor.initialize();
        return executor;
    }
}
