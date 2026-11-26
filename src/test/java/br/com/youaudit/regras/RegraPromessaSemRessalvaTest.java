package br.com.youaudit.regras;

import br.com.youaudit.avaliacao.AchadoDetectado;
import br.com.youaudit.baseline.BuscadorTermos;
import br.com.youaudit.extracao.TipoAfirmacao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static br.com.youaudit.regras.TermosDeTeste.afirmacao;
import static br.com.youaudit.regras.TermosDeTeste.contexto;
import static org.assertj.core.api.Assertions.assertThat;

/** Janela de 1 bloco (a do baseline-termos.json): ressalva no mesmo bloco, no anterior ou no seguinte vale. */
class RegraPromessaSemRessalvaTest {

    private final RegraPromessaSemRessalva regra = new RegraPromessaSemRessalva(
            TermosDeTeste.TERMOS.palavrasRessalva(), 1, new BuscadorTermos());

    @Test
    void promessaSemRessalvaGeraAchadoComOTrechoDaAfirmacao() {
        String pagina = "Método X\nFature R$10 mil por mês em 30 dias\nAulas gravadas\nSuporte por e-mail";
        List<AchadoDetectado> achados = regra.avaliar(contexto(pagina,
                List.of(afirmacao(2, TipoAfirmacao.RESULTADO_FINANCEIRO, "Fature R$10 mil por mês")), List.of()));

        assertThat(achados).extracting(AchadoDetectado::tipo).containsExactly(TipoAchado.PROMESSA_RESULTADO_SEM_RESSALVA);
        assertThat(achados.get(0).trechoPagina()).isEqualTo("Fature R$10 mil por mês");
    }

    @Test
    void ressalvaNoBlocoSeguinteConta() {
        String pagina = "Fature R$10 mil por mês\nResultados podem variar de pessoa para pessoa";
        assertThat(regra.avaliar(contexto(pagina,
                List.of(afirmacao(1, TipoAfirmacao.RESULTADO_FINANCEIRO, "Fature R$10 mil por mês")), List.of()))).isEmpty();
    }

    @Test
    void ressalvaLongeDaPromessaNaoConta() {
        String pagina = "Fature R$10 mil por mês\nb\nc\nd\nResultados podem variar";
        assertThat(regra.avaliar(contexto(pagina,
                List.of(afirmacao(1, TipoAfirmacao.PRAZO_RESULTADO, "R$10 mil por mês")), List.of()))).hasSize(1);
    }

    @Test
    void outrosTiposNaoSaoPromessaDeResultado() {
        String pagina = "Garantia de 7 dias";
        assertThat(regra.avaliar(contexto(pagina,
                List.of(afirmacao(1, TipoAfirmacao.GARANTIA, "Garantia de 7 dias")), List.of()))).isEmpty();
    }
}
