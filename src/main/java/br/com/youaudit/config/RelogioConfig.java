package br.com.youaudit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Fornece o relógio do sistema como um bean. Em vez de chamar Instant.now() direto, as classes recebem um Clock:
 * assim os testes podem usar um relógio fixo e a data fica previsível.
 */
@Configuration
public class RelogioConfig {

    @Bean
    public Clock relogio() {
        return Clock.systemDefaultZone();
    }
}
