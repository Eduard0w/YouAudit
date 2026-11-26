package br.com.youaudit.regras;

import br.com.youaudit.avaliacao.AchadoDetectado;
import br.com.youaudit.baseline.BuscadorTermos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static br.com.youaudit.regras.TermosDeTeste.contexto;
import static org.assertj.core.api.Assertions.assertThat;

class RegraTermoProibidoTest {

    private final RegraTermoProibido regra =
            new RegraTermoProibido(TermosDeTeste.TERMOS.termosProibidos(), new BuscadorTermos());

    @Test
    void achaTermoDaListaComOTrechoComoEstaNaPagina() {
        List<AchadoDetectado> achados = regra.avaliar(contexto("Curso de vendas\nRenda GARANTIDA já no primeiro mês", List.of(), List.of()));

        assertThat(achados).hasSize(1);
        assertThat(achados.get(0).tipo()).isEqualTo(TipoAchado.TERMO_PROIBIDO);
        assertThat(achados.get(0).trechoPagina()).isEqualTo("Renda GARANTIDA"); // literal, com a caixa da página
    }

    @Test
    void procuraNaPaginaInteiraMesmoSemAfirmacaoDaIa() {
        assertThat(regra.avaliar(contexto("Linha 1\nLinha 2\nMétodo sem esforço", List.of(), List.of()))).hasSize(1);
    }

    @Test
    void paginaLimpaNaoGeraAchado() {
        assertThat(regra.avaliar(contexto("Curso de bolos com 10 aulas\nGarantia de 7 dias", List.of(), List.of()))).isEmpty();
    }

    @Test
    void naoCasaPedacoDePalavra() {
        // "milagrosa" está na lista; "milagrosamente" não é o termo
        assertThat(regra.avaliar(contexto("Funcionou milagrosamente bem", List.of(), List.of()))).isEmpty();
    }
}
