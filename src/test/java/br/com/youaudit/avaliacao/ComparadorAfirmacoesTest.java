package br.com.youaudit.avaliacao;

import br.com.youaudit.coleta.Origem;
import br.com.youaudit.extracao.TipoAfirmacao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComparadorAfirmacoesTest {

    private static RotuloAfirmacao ouro(TipoAfirmacao tipo, String trecho) {
        return new RotuloAfirmacao("a", Origem.REAL, "pagina-01", tipo, trecho, "DESENVOLVIMENTO");
    }

    private static AfirmacaoDetectada achada(TipoAfirmacao tipo, String trecho) {
        return new AfirmacaoDetectada(1, tipo, trecho);
    }

    @Test
    void pedacoDaFraseDoGabaritoComMesmoTipoCasa() {
        var c = ComparadorAfirmacoes.comparar(List.of(ouro(TipoAfirmacao.GARANTIA, "Garantia de 7 dias para devolução")),
                List.of(achada(TipoAfirmacao.GARANTIA, "Garantia de 7 dias")));
        assertThat(c).isEqualTo(new ComparadorAfirmacoes.Contagem(1, 0, 0));
    }

    @Test
    void tipoDiferenteNaoCasa() {
        var c = ComparadorAfirmacoes.comparar(List.of(ouro(TipoAfirmacao.GARANTIA, "Garantia de 7 dias")),
                List.of(achada(TipoAfirmacao.ACESSO, "Garantia de 7 dias")));
        assertThat(c).isEqualTo(new ComparadorAfirmacoes.Contagem(0, 1, 1));
    }

    @Test
    void tresPedacosDaMesmaFraseContamUmAcertoEDoisFalsosPositivos() {
        var c = ComparadorAfirmacoes.comparar(List.of(ouro(TipoAfirmacao.BONUS, "Bônus 1 e Bônus 2 e Bônus 3")),
                List.of(achada(TipoAfirmacao.BONUS, "Bônus 1"), achada(TipoAfirmacao.BONUS, "Bônus 2"),
                        achada(TipoAfirmacao.BONUS, "Bônus 3")));
        assertThat(c).isEqualTo(new ComparadorAfirmacoes.Contagem(1, 2, 0));
    }

    @Test
    void comparacaoEExataSemIgnorarCaixa() {
        var c = ComparadorAfirmacoes.comparar(List.of(ouro(TipoAfirmacao.CERTIFICADO, "Certificado de conclusão")),
                List.of(achada(TipoAfirmacao.CERTIFICADO, "certificado de conclusão")));
        assertThat(c.vp()).isZero();
    }

    @Test
    void metricaSemDenominadorFicaNulaENaoZero() {
        Metricas m = Metricas.de(0, 0, 0);
        assertThat(m.precisao()).isNull();
        assertThat(m.cobertura()).isNull();
        assertThat(m.f1()).isNull();
        Metricas n = Metricas.de(3, 1, 2);
        assertThat(n.precisao()).isEqualTo(0.75);
        assertThat(n.cobertura()).isEqualTo(0.6);
        assertThat(n.f1()).isCloseTo(0.6667, org.assertj.core.data.Offset.offset(0.0001));
    }
}
