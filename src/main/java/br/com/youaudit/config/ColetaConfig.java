package br.com.youaudit.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Liga os records de configuração às linhas "coleta.*" e "lote.*" do application.properties. */
@Configuration
@EnableConfigurationProperties({ColetaPropriedades.class, LotePropriedades.class})
public class ColetaConfig {
}
