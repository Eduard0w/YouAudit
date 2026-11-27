package br.com.youaudit.revisao;

import br.com.youaudit.regras.EstadoAchado;
import br.com.youaudit.regras.TipoAchado;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComparadorVersoesTest {

    private final ComparadorVersoes comparador = new ComparadorVersoes();

    @Test
    void alertaIdenticoDepoisDePedidoDeAjusteContinua() {
        var v1 = alerta(1L, TipoAchado.TERMO_PROIBIDO, "Dinheiro fácil", null, "d", EstadoAchado.CONFIRMADO);
        var v2 = alerta(2L, TipoAchado.TERMO_PROIBIDO, "Dinheiro fácil", null, "d", EstadoAchado.PENDENTE);

        var itens = comparador.comparar(List.of(v1), List.of(v2), true);

        assertThat(itens).singleElement().satisfies(i -> {
            assertThat(i.situacao()).isEqualTo(SituacaoAlerta.CONTINUA);
            assertThat(i.anterior().estado()).isEqualTo(EstadoAchado.CONFIRMADO);
            // o comparador não herda a decisão: o alerta novo segue PENDENTE
            assertThat(i.atual().estado()).isEqualTo(EstadoAchado.PENDENTE);
        });
    }

    @Test
    void alertaIdenticoSemPedidoDeAjusteRecebeRotuloNeutro() {
        var v1 = alerta(1L, TipoAchado.TERMO_PROIBIDO, "Dinheiro fácil", null, "d", EstadoAchado.PENDENTE);
        var v2 = alerta(2L, TipoAchado.TERMO_PROIBIDO, "Dinheiro fácil", null, "d", EstadoAchado.PENDENTE);

        assertThat(comparador.comparar(List.of(v1), List.of(v2), false))
                .extracting(ComparadorVersoes.Item::situacao).containsExactly(SituacaoAlerta.IGUAL_ANTERIOR);
    }

    @Test
    void alertaQueSumiuFoiResolvido() {
        var v1 = alerta(1L, TipoAchado.QUANTIDADE_DIVERGENTE, "8 módulos completos", null, "d", EstadoAchado.PENDENTE);

        assertThat(comparador.comparar(List.of(v1), List.of(), true)).singleElement().satisfies(i -> {
            assertThat(i.situacao()).isEqualTo(SituacaoAlerta.RESOLVIDO);
            assertThat(i.atual()).isNull();
        });
    }

    @Test
    void fraseReescritaAindaSinalizadaENovaEAAntigaResolvida() {
        var v1 = alerta(1L, TipoAchado.PROMESSA_RESULTADO_SEM_RESSALVA, "Ganhe R$10 mil em 30 dias", null, "d", EstadoAchado.PENDENTE);
        var v2 = alerta(2L, TipoAchado.PROMESSA_RESULTADO_SEM_RESSALVA, "Ganhe até R$10 mil em 30 dias", null, "d", EstadoAchado.PENDENTE);

        assertThat(comparador.comparar(List.of(v1), List.of(v2), true))
                .extracting(ComparadorVersoes.Item::situacao)
                .containsExactlyInAnyOrder(SituacaoAlerta.NOVO, SituacaoAlerta.RESOLVIDO);
    }

    @Test
    void diferencaDeUmCaractereNaoEOMesmoAlerta() {
        var v1 = alerta(1L, TipoAchado.TERMO_PROIBIDO, "Dinheiro fácil", null, "d", EstadoAchado.PENDENTE);
        var v2 = alerta(2L, TipoAchado.TERMO_PROIBIDO, "dinheiro fácil", null, "d", EstadoAchado.PENDENTE);

        assertThat(comparador.comparar(List.of(v1), List.of(v2), true))
                .extracting(ComparadorVersoes.Item::situacao)
                .containsExactlyInAnyOrder(SituacaoAlerta.NOVO, SituacaoAlerta.RESOLVIDO);
    }

    @Test
    void mesmoTrechoComEntregaDiferenteNaoEOMesmoAlerta() {
        var v1 = alerta(1L, TipoAchado.CUSTO_OMITIDO, null, "Requer assinatura da plataforma W", "d", EstadoAchado.PENDENTE);
        var v2 = alerta(2L, TipoAchado.CUSTO_OMITIDO, null, "Requer assinatura da plataforma Z", "d", EstadoAchado.PENDENTE);

        assertThat(comparador.comparar(List.of(v1), List.of(v2), true))
                .extracting(ComparadorVersoes.Item::situacao)
                .containsExactlyInAnyOrder(SituacaoAlerta.NOVO, SituacaoAlerta.RESOLVIDO);
    }

    @Test
    void alertaSemTrechoEComparadoPeloDetalhe() {
        var semPreco = alerta(1L, TipoAchado.INFO_ESSENCIAL_AUSENTE, null, null, "Falta o preço", EstadoAchado.PENDENTE);
        var semContato = alerta(2L, TipoAchado.INFO_ESSENCIAL_AUSENTE, null, null, "Falta o canal de contato", EstadoAchado.PENDENTE);
        var semPrecoDeNovo = alerta(3L, TipoAchado.INFO_ESSENCIAL_AUSENTE, null, null, "Falta o preço", EstadoAchado.PENDENTE);

        var itens = comparador.comparar(List.of(semPreco, semContato), List.of(semPrecoDeNovo), true);

        assertThat(itens).extracting(ComparadorVersoes.Item::situacao)
                .containsExactlyInAnyOrder(SituacaoAlerta.CONTINUA, SituacaoAlerta.RESOLVIDO);
        assertThat(itens).filteredOn(i -> i.situacao() == SituacaoAlerta.RESOLVIDO)
                .singleElement().satisfies(i -> assertThat(i.anterior().detalhe()).isEqualTo("Falta o canal de contato"));
    }

    @Test
    void cadaAlertaAnteriorCasaNoMaximoUmaVez() {
        var a = alerta(1L, TipoAchado.TERMO_PROIBIDO, "garantido", null, "d", EstadoAchado.PENDENTE);
        var b = alerta(2L, TipoAchado.TERMO_PROIBIDO, "garantido", null, "d", EstadoAchado.PENDENTE);
        var c = alerta(3L, TipoAchado.TERMO_PROIBIDO, "garantido", null, "d", EstadoAchado.PENDENTE);

        assertThat(comparador.comparar(List.of(a, b), List.of(c), true))
                .extracting(ComparadorVersoes.Item::situacao)
                .containsExactlyInAnyOrder(SituacaoAlerta.CONTINUA, SituacaoAlerta.RESOLVIDO);
    }

    @Test
    void contagemSemJulgamentoAnteriorSomaNovosEOsQueContinuamPendentes() {
        var confirmado = alerta(1L, TipoAchado.TERMO_PROIBIDO, "x", null, "d", EstadoAchado.CONFIRMADO);
        var pendente = alerta(2L, TipoAchado.TERMO_PROIBIDO, "y", null, "d", EstadoAchado.PENDENTE);
        var resolvido = alerta(3L, TipoAchado.QUANTIDADE_DIVERGENTE, "z", null, "d", EstadoAchado.PENDENTE);
        var itens = comparador.comparar(List.of(confirmado, pendente, resolvido), List.of(
                alerta(4L, TipoAchado.TERMO_PROIBIDO, "x", null, "d", EstadoAchado.PENDENTE),
                alerta(5L, TipoAchado.TERMO_PROIBIDO, "y", null, "d", EstadoAchado.PENDENTE),
                alerta(6L, TipoAchado.CUSTO_OMITIDO, null, "w", "d", EstadoAchado.PENDENTE)), true);

        var c = ServicoConsulta.contar(itens);

        assertThat(c.resolvidos()).isEqualTo(1);
        assertThat(c.continuam()).isEqualTo(2);
        assertThat(c.novos()).isEqualTo(1);
        assertThat(c.alertasAnterior()).isEqualTo(3);
        assertThat(c.alertasAtual()).isEqualTo(3);
        // "x" já foi julgado (CONFIRMADO) na v1; "y" ficou pendente; o CUSTO_OMITIDO é novo
        assertThat(c.semJulgamentoAnterior()).isEqualTo(2);
    }

    private static ComparadorVersoes.Alerta alerta(Long id, TipoAchado tipo, String pagina, String entrega,
                                                   String detalhe, EstadoAchado estado) {
        return new ComparadorVersoes.Alerta(id, tipo, pagina, entrega, detalhe, estado, null);
    }
}
