package br.com.youaudit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tamanhos usados na preparação do texto (linhas "extracao.*").
 *
 * @param maxCaracteresBloco   um parágrafo maior que isto é cortado em mais de um bloco
 * @param maxCaracteresChamada soma máxima dos blocos enviados numa única chamada à IA
 */
@ConfigurationProperties(prefix = "extracao")
public record ExtracaoPropriedades(int maxCaracteresBloco, int maxCaracteresChamada) {
}
