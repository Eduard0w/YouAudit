package br.com.youaudit.avaliacao;

import br.com.youaudit.coleta.Origem;
import br.com.youaudit.extracao.TipoAfirmacao;

/** Uma linha de data/real/rotulos/afirmacoes.csv (rotulada pelo Eduardo; o código nunca altera). */
public record RotuloAfirmacao(String id, Origem origem, String paginaId, TipoAfirmacao tipo, String trecho, String divisao) {
}
