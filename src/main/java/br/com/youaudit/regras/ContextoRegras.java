package br.com.youaudit.regras;

import br.com.youaudit.avaliacao.AfirmacaoDetectada;
import br.com.youaudit.avaliacao.ItemEntrega;
import br.com.youaudit.extracao.Bloco;

import java.util.List;

/**
 * Tudo o que uma {@link RegraDeterministica} pode olhar. É só leitura: as regras não chamam a IA nem o banco.
 *
 * @param blocos      a página dividida em blocos (os mesmos que foram para a IA, para o blocoId bater)
 * @param afirmacoes  as afirmações que a IA extraiu E o código já conferiu (trecho literal); nunca a resposta crua
 * @param itens       a entrega cadastrada; lista vazia = sem entrega (as regras que dependem dela não rodam)
 */
public record ContextoRegras(List<Bloco> blocos, List<AfirmacaoDetectada> afirmacoes, List<ItemEntrega> itens) {

    public ContextoRegras {
        blocos = blocos == null ? List.of() : List.copyOf(blocos);
        afirmacoes = afirmacoes == null ? List.of() : List.copyOf(afirmacoes);
        itens = itens == null ? List.of() : List.copyOf(itens);
    }

    public boolean temEntrega() {
        return !itens.isEmpty();
    }
}
