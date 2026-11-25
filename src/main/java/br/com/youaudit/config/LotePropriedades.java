package br.com.youaudit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 * Configuração do lote de links reais (linhas "lote.*" do application.properties).
 *
 * @param arquivoUrls arquivo com uma linha "pagina-NN URL" por página (em data/real, fora do Git)
 * @param intervaloMs espera entre um evento e o próximo (1 000 = 1 por segundo)
 */
@ConfigurationProperties(prefix = "lote")
public record LotePropriedades(Path arquivoUrls, long intervaloMs) {
}
