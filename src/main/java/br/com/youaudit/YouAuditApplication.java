package br.com.youaudit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada do YouAudit.
 * {@code @SpringBootApplication} liga a configuração automática e procura componentes
 * (controllers, serviços, repositórios) em todos os subpacotes de {@code br.com.youaudit}.
 */
@SpringBootApplication
public class YouAuditApplication {

    public static void main(String[] args) {
        SpringApplication.run(YouAuditApplication.class, args);
    }
}
