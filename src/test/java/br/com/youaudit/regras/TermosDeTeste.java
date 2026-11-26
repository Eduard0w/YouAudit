package br.com.youaudit.regras;

import br.com.youaudit.avaliacao.AfirmacaoDetectada;
import br.com.youaudit.avaliacao.ItemEntrega;
import br.com.youaudit.baseline.TermosBaseline;
import br.com.youaudit.config.BaselineConfig;
import br.com.youaudit.config.ExtracaoPropriedades;
import br.com.youaudit.extracao.Bloco;
import br.com.youaudit.extracao.PreparadorBlocos;
import br.com.youaudit.extracao.TipoAfirmacao;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/** Peças comuns dos testes das regras: as listas REAIS do baseline-termos.json e atalhos para montar o contexto. */
final class TermosDeTeste {

    static final TermosBaseline TERMOS = new BaselineConfig().termosBaseline(new ObjectMapper());
    private static final PreparadorBlocos PREPARADOR = new PreparadorBlocos(new ExtracaoPropriedades(1200, 8000));

    private TermosDeTeste() {
    }

    static List<Bloco> blocos(String pagina) {
        return PREPARADOR.dividir(pagina);
    }

    /** Uma afirmação no bloco de número {@code blocoId}. O trecho precisa existir nesse bloco (como no pipeline). */
    static AfirmacaoDetectada afirmacao(int blocoId, TipoAfirmacao tipo, String trecho) {
        return new AfirmacaoDetectada(blocoId, tipo, trecho);
    }

    static ContextoRegras contexto(String pagina, List<AfirmacaoDetectada> afirmacoes, List<ItemEntrega> itens) {
        return new ContextoRegras(blocos(pagina), afirmacoes, itens);
    }
}
