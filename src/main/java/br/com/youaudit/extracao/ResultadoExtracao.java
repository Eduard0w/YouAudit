package br.com.youaudit.extracao;

import java.util.List;

/**
 * Resumo de uma extração, em contagens (regra: contagens ao lado de qualquer porcentagem).
 *
 * @param blocos           em quantos blocos a página foi dividida
 * @param chamadasIa       quantas vezes a IA foi chamada de verdade (0 se tudo veio do cache)
 * @param respostasDoCache quantos grupos vieram do cache
 */
public record ResultadoExtracao(int blocos, int chamadasIa, int respostasDoCache, List<Afirmacao> aceitas,
                                List<AfirmacaoDescartada> descartadas) {
}
