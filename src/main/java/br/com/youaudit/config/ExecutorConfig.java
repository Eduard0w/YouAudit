package br.com.youaudit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Pool pequeno de threads para o trabalho em segundo plano (CONTRATOS.md §4: 2 a 4 threads).
 * Usado pelo lote de links, pelo simulador e pelo pipeline assíncrono. 3 threads: um lote/simulador esperando
 * entre eventos não trava as auditorias.
 */
@Configuration
public class ExecutorConfig {

    @Bean
    public ThreadPoolTaskExecutor executorAuditoria() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("auditoria-");
        executor.initialize();
        return executor;
    }
}
