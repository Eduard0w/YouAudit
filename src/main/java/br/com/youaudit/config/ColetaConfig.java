package br.com.youaudit.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Liga os records de configuração às linhas "coleta.*", "lote.*", "gemini.*" e "extracao.*" do application.properties. */
@Configuration
@EnableConfigurationProperties({ColetaPropriedades.class, LotePropriedades.class, GeminiPropriedades.class,
        ExtracaoPropriedades.class})
public class ColetaConfig {
}
