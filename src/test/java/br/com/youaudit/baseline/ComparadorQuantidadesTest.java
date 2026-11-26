package br.com.youaudit.baseline;

import br.com.youaudit.avaliacao.AchadoDetectado;
import br.com.youaudit.avaliacao.ItemEntrega;
import br.com.youaudit.coleta.TipoConteudo;
import br.com.youaudit.regras.TipoAchado;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static br.com.youaudit.baseline.Comparacao.EXATO;
import static br.com.youaudit.baseline.Comparacao.MAIS_QUE;
import static br.com.youaudit.baseline.Comparacao.PELO_MENOS;
import static br.com.youaudit.baseline.UnidadeQuantidade.AULA;
import static br.com.youaudit.baseline.UnidadeQuantidade.MODULO;
import static org.assertj.core.api.Assertions.assertThat;

/** Casos da especificação (youaudit-docs/ESPEC-baseline-eduardo.md, peça 3). */
public class ComparadorQuantidadesTest {

    private final ComparadorQuantidades comparador = new ComparadorQuantidades();

    private static List<ItemEntrega> itens(TipoConteudo tipo, int quantos) {
        List<ItemEntrega> itens = new ArrayList<>();
        for (int i = 1; i <= quantos; i++) {
            itens.add(new ItemEntrega(tipo, tipo.name() + " " + i, null));
        }
        return itens;
    }

    private List<AchadoDetectado> comparar(QuantidadeEncontrada q, List<ItemEntrega> entrega) {
        return comparador.comparar(List.of(q), entrega);
    }

    @Test
    void exatoDiferenteGeraAchadoComDetalhe() {
        QuantidadeEncontrada q = new QuantidadeEncontrada(40, AULA, EXATO, "40 aulas");

        assertThat(comparar(q, itens(TipoConteudo.AULA, 28))).containsExactly(new AchadoDetectado(
                TipoAchado.QUANTIDADE_DIVERGENTE, "40 aulas", null,
                "Possivelmente em desacordo com a entrega: a página diz \"40 aulas\" e a entrega cadastrada tem 28 aulas."));
    }

    @Test
    void exatoIgualNaoGeraAchado() {
        assertThat(comparar(new QuantidadeEncontrada(28, AULA, EXATO, "28 aulas"), itens(TipoConteudo.AULA, 28))).isEmpty();
    }

    @Test
    void peloMenos() {
        QuantidadeEncontrada q = new QuantidadeEncontrada(40, AULA, PELO_MENOS, "+40 aulas");

        assertThat(comparar(q, itens(TipoConteudo.AULA, 40))).isEmpty();
        assertThat(comparar(q, itens(TipoConteudo.AULA, 39))).hasSize(1);
    }

    @Test
    void maisQueEhEstrito() {
        QuantidadeEncontrada q = new QuantidadeEncontrada(40, AULA, MAIS_QUE, "mais de 40 aulas");

        assertThat(comparar(q, itens(TipoConteudo.AULA, 41))).isEmpty();
        assertThat(comparar(q, itens(TipoConteudo.AULA, 40))).hasSize(1);
    }

    @Test
    void aulasNaoContamComoModulo() {
        List<ItemEntrega> entrega = new ArrayList<>(itens(TipoConteudo.MODULO, 5));
        entrega.addAll(itens(TipoConteudo.AULA, 28));

        assertThat(comparar(new QuantidadeEncontrada(5, MODULO, EXATO, "5 módulos"), entrega)).isEmpty();
    }

    @Test
    void singularNoDetalhe() {
        assertThat(comparar(new QuantidadeEncontrada(2, AULA, EXATO, "2 aulas"), itens(TipoConteudo.AULA, 1)))
                .singleElement().extracting(AchadoDetectado::detalhe).asString().endsWith("tem 1 aula.");
        assertThat(comparar(new QuantidadeEncontrada(2, MODULO, EXATO, "2 módulos"), itens(TipoConteudo.MODULO, 1)))
                .singleElement().extracting(AchadoDetectado::detalhe).asString().endsWith("tem 1 módulo.");
    }

    @Test
    void semItensDaUnidadeNaoCompara() {
        assertThat(comparar(new QuantidadeEncontrada(40, AULA, EXATO, "40 aulas"), itens(TipoConteudo.BONUS, 3))).isEmpty();
    }

    @Test
    void repetidaNaPaginaViraUmAchado() {
        QuantidadeEncontrada q = new QuantidadeEncontrada(40, AULA, EXATO, "40 aulas");

        assertThat(comparador.comparar(List.of(q, q), itens(TipoConteudo.AULA, 28))).hasSize(1);
    }

    @Test
    void entradasVaziasOuNulas() {
        QuantidadeEncontrada q = new QuantidadeEncontrada(40, AULA, EXATO, "40 aulas");

        assertThat(comparador.comparar(List.of(), itens(TipoConteudo.AULA, 28))).isEmpty();
        assertThat(comparador.comparar(null, itens(TipoConteudo.AULA, 28))).isEmpty();
        assertThat(comparador.comparar(List.of(q), List.of())).isEmpty();
        assertThat(comparador.comparar(List.of(q), null)).isEmpty();
    }
}
