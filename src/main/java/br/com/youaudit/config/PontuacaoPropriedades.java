package br.com.youaudit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limites das faixas (linhas "pontuacao.*"). CONTRATOS.md §2: BAIXA 0–29, MEDIA 30–59, ALTA 60–100.
 * Ficam na configuração e não no código (regra do CLAUDE.md), como os pesos, que ficam na tabela {@code regra}.
 *
 * @param inicioMedia primeira pontuação da faixa MEDIA (30)
 * @param inicioAlta  primeira pontuação da faixa ALTA (60)
 * @param maximo      teto da soma (100)
 */
@ConfigurationProperties(prefix = "pontuacao")
public record PontuacaoPropriedades(int inicioMedia, int inicioAlta, int maximo) {
}
