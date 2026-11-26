package br.com.youaudit.avaliacao;

import br.com.youaudit.regras.TipoAchado;

/**
 * Um achado: indício de que a página está possivelmente em desacordo com uma regra ou com a entrega.
 *
 * <p>{@code trechoPagina} e {@code trechoEntrega} são literais quando existem. Podem ser nulos quando o achado é uma
 * AUSÊNCIA (ex.: INFO_ESSENCIAL_AUSENTE não tem trecho, porque o problema é o que não está escrito) ou quando a
 * evidência é uma contagem (QUANTIDADE_DIVERGENTE: "40 aulas" na página x 28 aulas cadastradas). {@code detalhe} explica
 * em uma frase, em linguagem de indício.
 */
public record AchadoDetectado(TipoAchado tipo, String trechoPagina, String trechoEntrega, String detalhe) {
}
