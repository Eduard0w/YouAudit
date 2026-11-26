package br.com.youaudit.regras;

import br.com.youaudit.avaliacao.AchadoDetectado;
import br.com.youaudit.avaliacao.ItemEntrega;
import br.com.youaudit.baseline.ComparadorQuantidades;
import br.com.youaudit.baseline.ExtratorNumeros;
import br.com.youaudit.coleta.TipoConteudo;
import br.com.youaudit.extracao.TipoAfirmacao;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static br.com.youaudit.regras.TermosDeTeste.afirmacao;
import static br.com.youaudit.regras.TermosDeTeste.contexto;
import static org.assertj.core.api.Assertions.assertThat;

class RegraQuantidadeDivergenteTest {

    private final RegraQuantidadeDivergente regra = new RegraQuantidadeDivergente(
            new ExtratorNumeros(TermosDeTeste.TERMOS.unidades()), new ComparadorQuantidades());

    @Test
    void quarentaAulasNaPaginaEVinteEOitoNaEntregaDiverge() {
        List<AchadoDetectado> achados = regra.avaliar(contexto("São 40 aulas práticas",
                List.of(afirmacao(1, TipoAfirmacao.QUANTIDADE_CONTEUDO, "São 40 aulas práticas")), aulas(28)));

        assertThat(achados).extracting(AchadoDetectado::tipo).containsExactly(TipoAchado.QUANTIDADE_DIVERGENTE);
        assertThat(achados.get(0).trechoPagina()).isEqualTo("40 aulas");
    }

    @Test
    void mesmaQuantidadeNaoDiverge() {
        assertThat(regra.avaliar(contexto("São 28 aulas",
                List.of(afirmacao(1, TipoAfirmacao.QUANTIDADE_CONTEUDO, "28 aulas")), aulas(28)))).isEmpty();
    }

    @Test
    void semEntregaCadastradaARegraNaoRoda() {
        assertThat(regra.avaliar(contexto("São 40 aulas",
                List.of(afirmacao(1, TipoAfirmacao.QUANTIDADE_CONTEUDO, "40 aulas")), List.of()))).isEmpty();
    }

    @Test
    void numeroForaDeAfirmacaoDeQuantidadeNaoEComparado() {
        // A IA não marcou como QUANTIDADE_CONTEUDO: a regra só olha o que a IA apontou como quantidade.
        assertThat(regra.avaliar(contexto("Mais de 40 aulas de alunos satisfeitos",
                List.of(afirmacao(1, TipoAfirmacao.DEPOIMENTO_RESULTADO, "40 aulas de alunos satisfeitos")), aulas(28))))
                .isEmpty();
    }

    private static List<ItemEntrega> aulas(int quantidade) {
        List<ItemEntrega> itens = new ArrayList<>();
        for (int i = 1; i <= quantidade; i++) {
            itens.add(new ItemEntrega(TipoConteudo.AULA, "Aula " + i, null));
        }
        return itens;
    }
}
