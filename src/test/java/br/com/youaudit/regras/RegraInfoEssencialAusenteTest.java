package br.com.youaudit.regras;

import br.com.youaudit.avaliacao.AchadoDetectado;
import br.com.youaudit.baseline.BuscadorTermos;
import br.com.youaudit.extracao.TipoAfirmacao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static br.com.youaudit.regras.TermosDeTeste.afirmacao;
import static br.com.youaudit.regras.TermosDeTeste.contexto;
import static org.assertj.core.api.Assertions.assertThat;

class RegraInfoEssencialAusenteTest {

    private final RegraInfoEssencialAusente regra =
            new RegraInfoEssencialAusente(TermosDeTeste.TERMOS.informacoesEssenciais(), new BuscadorTermos());

    @Test
    void paginaComTudoNaoGeraAchado() {
        String pagina = "Preço: R$ 97 à vista ou no pix\nAcesso vitalício\nSuporte por e-mail\nGarantia de 7 dias";
        assertThat(regra.avaliar(contexto(pagina, List.of(), List.of()))).isEmpty();
    }

    @Test
    void cadaInformacaoQueFaltaViraUmAchadoSemTrecho() {
        List<AchadoDetectado> achados = regra.avaliar(contexto("Aprenda a fazer bolos", List.of(), List.of()));

        assertThat(achados).hasSize(4).allSatisfy(a -> {
            assertThat(a.tipo()).isEqualTo(TipoAchado.INFO_ESSENCIAL_AUSENTE);
            assertThat(a.trechoPagina()).isNull();
        });
    }

    @Test
    void afirmacaoDaIaComprovaAInformacaoMesmoSemPalavraDaLista() {
        // "devolvemos 100% do seu dinheiro" não tem "reembolso"/"garantia", mas a IA classificou como GARANTIA.
        String pagina = "Preço: R$ 97 no pix\nAcesso vitalício\nSuporte por e-mail\nSe não gostar, devolvemos 100% do seu dinheiro";
        List<AchadoDetectado> achados = regra.avaliar(contexto(pagina,
                List.of(afirmacao(4, TipoAfirmacao.GARANTIA, "devolvemos 100% do seu dinheiro")), List.of()));

        assertThat(achados).isEmpty();
    }

    @Test
    void semAfirmacaoDaIaEsemPalavraAInformacaoFalta() {
        String pagina = "Preço: R$ 97 no pix\nAcesso vitalício\nSuporte por e-mail\nSe não gostar, devolvemos 100% do seu dinheiro";
        assertThat(regra.avaliar(contexto(pagina, List.of(), List.of())))
                .extracting(AchadoDetectado::detalhe).containsExactly("Não encontrei na página: política de reembolso.");
    }
}
