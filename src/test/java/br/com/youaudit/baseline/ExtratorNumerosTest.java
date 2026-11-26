package br.com.youaudit.baseline;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static br.com.youaudit.baseline.Comparacao.EXATO;
import static br.com.youaudit.baseline.Comparacao.MAIS_QUE;
import static br.com.youaudit.baseline.Comparacao.PELO_MENOS;
import static br.com.youaudit.baseline.UnidadeQuantidade.AULA;
import static br.com.youaudit.baseline.UnidadeQuantidade.MODULO;
import static org.assertj.core.api.Assertions.assertThat;

/** Casos da especificação (youaudit-docs/ESPEC-baseline-eduardo.md, peça 2). */
public class ExtratorNumerosTest {

    private final ExtratorNumeros extrator = new ExtratorNumeros(Map.of(
            AULA, List.of("aula", "aulas", "videoaula", "videoaulas"),
            MODULO, List.of("modulo", "modulos")));

    private QuantidadeEncontrada unica(String texto) {
        List<QuantidadeEncontrada> r = extrator.extrair(texto);
        assertThat(r).hasSize(1);
        return r.get(0);
    }

    @Test
    void numeroEUnidadeSimples() {
        assertThat(extrator.extrair("São 40 aulas gravadas"))
                .containsExactly(new QuantidadeEncontrada(40, AULA, EXATO, "40 aulas"));
    }

    @Test
    void trechoMantemACaixaOriginal() {
        assertThat(unica("40 Aulas").trecho()).isEqualTo("40 Aulas");
    }

    @Test
    void unidadeComOuSemAcento() {
        assertThat(extrator.extrair("5 módulos")).containsExactly(new QuantidadeEncontrada(5, MODULO, EXATO, "5 módulos"));
        assertThat(extrator.extrair("5 modulos")).containsExactly(new QuantidadeEncontrada(5, MODULO, EXATO, "5 modulos"));
    }

    @Test
    void singular() {
        assertThat(extrator.extrair("1 aula")).containsExactly(new QuantidadeEncontrada(1, AULA, EXATO, "1 aula"));
    }

    @Test
    void sinonimoDaUnidade() {
        assertThat(unica("40 videoaulas")).isEqualTo(new QuantidadeEncontrada(40, AULA, EXATO, "40 videoaulas"));
    }

    @Test
    void maisDeEhMaisQue() {
        assertThat(unica("mais de 40 aulas")).isEqualTo(new QuantidadeEncontrada(40, AULA, MAIS_QUE, "mais de 40 aulas"));
        assertThat(unica("Mais De 40 aulas")).isEqualTo(new QuantidadeEncontrada(40, AULA, MAIS_QUE, "Mais De 40 aulas"));
    }

    @Test
    void maisSinalEhPeloMenos() {
        assertThat(unica("+40 aulas")).isEqualTo(new QuantidadeEncontrada(40, AULA, PELO_MENOS, "+40 aulas"));
        assertThat(unica("+ 40 aulas")).isEqualTo(new QuantidadeEncontrada(40, AULA, PELO_MENOS, "+ 40 aulas"));
    }

    @Test
    void peloMenosEhPeloMenos() {
        assertThat(unica("pelo menos 12 módulos"))
                .isEqualTo(new QuantidadeEncontrada(12, MODULO, PELO_MENOS, "pelo menos 12 módulos"));
    }

    @Test
    void separadorDeMilhar() {
        assertThat(unica("1.200 aulas").valor()).isEqualTo(1200);
        assertThat(unica("1.000.000 aulas").valor()).isEqualTo(1_000_000);
    }

    @Test
    void variosEspacosFicamNoTrecho() {
        assertThat(unica("40  aulas").trecho()).isEqualTo("40  aulas");
    }

    @Test
    void pontoFinalNaoEntraNoTrecho() {
        assertThat(unica("40 aulas.").trecho()).isEqualTo("40 aulas");
    }

    @Test
    void naoCortaONumero() {
        assertThat(unica("140 aulas").valor()).isEqualTo(140);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1,5 aulas", "v2.40 aulas", "40aulas", "40 aulasss", "aula 12", "12ª aula",
            "R$ 40", "40%", "40 alunos", "12 meses de acesso", "99999999999 aulas", "x40 aulas"})
    void naoExtraiNada(String texto) {
        assertThat(extrator.extrair(texto)).isEmpty();
    }

    @Test
    void ateFicaExato() {
        assertThat(extrator.extrair("até 40 aulas")).containsExactly(new QuantidadeEncontrada(40, AULA, EXATO, "40 aulas"));
    }

    @Test
    void doisResultadosNaOrdemDoTexto() {
        assertThat(extrator.extrair("10 módulos e 80 aulas")).containsExactly(
                new QuantidadeEncontrada(10, MODULO, EXATO, "10 módulos"),
                new QuantidadeEncontrada(80, AULA, EXATO, "80 aulas"));
    }

    @Test
    void textoNuloOuVazio() {
        assertThat(extrator.extrair(null)).isEmpty();
        assertThat(extrator.extrair("")).isEmpty();
    }

    @Test
    void semUnidadesConfiguradasNaoExtraiNada() {
        assertThat(new ExtratorNumeros(Map.of()).extrair("40 aulas")).isEmpty();
    }

    @Test
    void trechoEhSempreLiteral() {
        String texto = "Mais  de 1.200 VideoAulas, + 5 Módulos e pelo menos 3 aulas.";
        List<QuantidadeEncontrada> r = extrator.extrair(texto);

        assertThat(r).hasSize(3);
        r.forEach(q -> assertThat(texto).contains(q.trecho()));
    }
}
