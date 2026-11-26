package br.com.youaudit.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Liga os records de configuração às linhas "coleta.*", "lote.*", "gemini.*", "extracao.*" e "pontuacao.*" do application.properties. */
@Configuration
@EnableConfigurationProperties({ColetaPropriedades.class, LotePropriedades.class, GeminiPropriedades.class,
        ExtracaoPropriedades.class, PontuacaoPropriedades.class, PipelinePropriedades.class, GroqPropriedades.class})
public class ColetaConfig {
}
