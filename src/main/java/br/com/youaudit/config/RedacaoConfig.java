package br.com.youaudit.config;

import br.com.youaudit.redacao.ModelosRedacao;
import br.com.youaudit.regras.TipoAchado;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.Map;

/**
 * Lê {@code redacao-modelos.json} (os textos de reserva da redação). Falta um modelo? A aplicação não sobe:
 * sem modelo, um achado poderia ficar sem sugestão quando a IA falhar.
 */
@Configuration
public class RedacaoConfig {

    static final String ARQUIVO = "redacao-modelos.json";

    @Bean
    public ModelosRedacao modelosRedacao(ObjectMapper mapper) {
        try (InputStream entrada = new ClassPathResource(ARQUIVO).getInputStream()) {
            JsonNode modelos = mapper.readTree(entrada).path("modelos");
            Map<TipoAchado, String> porTipo = new EnumMap<>(TipoAchado.class);
            for (TipoAchado tipo : TipoAchado.values()) {
                String texto = modelos.path(tipo.name()).asText("");
                if (texto.isBlank()) {
                    throw new IllegalStateException(ARQUIVO + ": falta o modelo de " + tipo);
                }
                porTipo.put(tipo, texto);
            }
            return new ModelosRedacao(porTipo);
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui ler " + ARQUIVO, e);
        }
    }
}
