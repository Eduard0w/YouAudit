package br.com.youaudit.avaliacao;

import br.com.youaudit.extracao.TipoAfirmacao;

/** Uma afirmação achada na página. {@code trecho} é literal: existe caractere por caractere no bloco {@code blocoId}. */
public record AfirmacaoDetectada(int blocoId, TipoAfirmacao tipo, String trecho) {
}
