package br.com.youaudit.avaliacao;

import br.com.youaudit.coleta.TipoConteudo;

/** Um item da entrega (módulo, aula, bônus ou certificado), como o verificador enxerga. */
public record ItemEntrega(TipoConteudo tipo, String titulo, String descricao) {
}
