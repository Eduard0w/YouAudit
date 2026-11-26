package br.com.youaudit.regras;

import br.com.youaudit.avaliacao.AchadoDetectado;
import br.com.youaudit.extracao.TipoAfirmacao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static br.com.youaudit.regras.TermosDeTeste.afirmacao;
import static br.com.youaudit.regras.TermosDeTeste.contexto;
import static org.assertj.core.api.Assertions.assertThat;

class RegraReconhecimentoSemComprovanteTest {

    private final RegraReconhecimentoSemComprovante regra = new RegraReconhecimentoSemComprovante();

    @Test
    void reconhecimentoOficialSempreViraAchadoPorqueNaoHaCampoDeComprovante() {
        List<AchadoDetectado> achados = regra.avaliar(contexto("Curso reconhecido pelo MEC",
                List.of(afirmacao(1, TipoAfirmacao.RECONHECIMENTO_OFICIAL, "reconhecido pelo MEC")), List.of()));

        assertThat(achados).extracting(AchadoDetectado::tipo).containsExactly(TipoAchado.RECONHECIMENTO_SEM_COMPROVANTE);
        assertThat(achados.get(0).trechoPagina()).isEqualTo("reconhecido pelo MEC");
    }

    @Test
    void semReconhecimentoNaoGeraNada() {
        assertThat(regra.avaliar(contexto("Certificado de conclusão",
                List.of(afirmacao(1, TipoAfirmacao.CERTIFICADO, "Certificado de conclusão")), List.of()))).isEmpty();
    }
}
