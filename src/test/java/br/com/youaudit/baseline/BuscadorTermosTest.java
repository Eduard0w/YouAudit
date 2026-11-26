package br.com.youaudit.baseline;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Casos da especificação (youaudit-docs/ESPEC-baseline-eduardo.md, peça 1). */
public class BuscadorTermosTest {

    private final BuscadorTermos buscador = new BuscadorTermos();

    private List<String> trechos(String texto, String... termos) {
        return buscador.buscar(texto, List.of(termos)).stream().map(Ocorrencia::trecho).toList();
    }

    // Teste escrito pelo Eduardo.
    @Test
    void achaDuasOcorrenciasDoMesmoTermo() {
        var buscador = new BuscadorTermos();
        var texto = "Bônus: mentoria e Bônus extra";
        var r = buscador.buscar(texto, List.of("Bônus"));

        assertEquals(2, r.size());
        assertEquals("Bônus", r.get(0).trecho());
        assertEquals(0, r.get(0).inicio());
        for (Ocorrencia o : r) {
            assertEquals(texto.substring(o.inicio(), o.fim()), o.trecho());
        }
    }

    // Regra 3: caixa
    @Test
    void ignoraCaixaEDevolveOTrechoOriginal() {
        assertThat(buscador.buscar("Resultado GARANTIDO!", List.of("garantido")))
                .containsExactly(new Ocorrencia("garantido", 10, 19, "GARANTIDO"));
    }

    // Regra 4: acentos, nos dois lados
    @Test
    void termoComAcentoCasaTextoSemAcento() {
        assertThat(trechos("ganhe ja", "ganhe já")).containsExactly("ganhe ja");
    }

    @Test
    void termoSemAcentoCasaTextoComAcento() {
        assertThat(trechos("ganhe já", "ganhe ja")).containsExactly("ganhe já");
    }

    @Test
    void ignoraCedilhaETil() {
        assertThat(trechos("Aprovação imediata", "aprovacao")).containsExactly("Aprovação");
    }

    // Regra 5: palavra inteira
    @Test
    void naoCasaPedacoDePalavraNoFim() {
        assertThat(trechos("empresa garantidora", "garantido")).isEmpty();
    }

    @Test
    void naoCasaPedacoDePalavraNoComeco() {
        assertThat(trechos("ingarantido", "garantido")).isEmpty();
    }

    @Test
    void singularNaoCasaPlural() {
        assertThat(trechos("São 40 aulas", "aula")).isEmpty();
    }

    @Test
    void termoDeDuasPalavrasNaoCasaPedacoDaSegunda() {
        assertThat(trechos("renda extraordinária", "renda extra")).isEmpty();
    }

    // Regra 6: espaços
    @Test
    void variosEspacosCasamUmEspacoDoTermo() {
        assertThat(trechos("renda   extra", "renda extra")).containsExactly("renda   extra");
    }

    @Test
    void espacoInseparavelCasaEspacoDoTermo() {
        assertThat(trechos("renda extra", "renda extra")).containsExactly("renda extra");
    }

    @Test
    void tabCasaEspacoDoTermo() {
        assertThat(trechos("renda\textra", "renda extra")).containsExactly("renda\textra");
    }

    // Regra 7: caracteres especiais são literais
    @Test
    void porcentagemEhLiteral() {
        assertThat(trechos("Satisfação 100% garantida", "100%")).containsExactly("100%");
    }

    @Test
    void hifenEhLiteral() {
        assertThat(trechos("Baixe o e-book", "e-book")).containsExactly("e-book");
    }

    @Test
    void parentesesNaoViramRegex() {
        assertThat(trechos("Ganhe (grátis) hoje", "(grátis)")).containsExactly("(grátis)");
    }

    @Test
    void pontoDoTermoNaoCasaQualquerCaractere() {
        assertThat(trechos("versao 2x0", "2.0")).isEmpty();
    }

    // Regra 9: sobreposição e repetição
    @Test
    void termosQueSeSobrepoemAparecemOsDoisNaOrdem() {
        assertThat(buscador.buscar("lucro garantido", List.of("garantido", "lucro garantido"))).containsExactly(
                new Ocorrencia("lucro garantido", 0, 15, "lucro garantido"),
                new Ocorrencia("garantido", 6, 15, "garantido"));
    }

    @Test
    void termosIguaisDepoisDeNormalizarNaoDuplicam() {
        assertThat(trechos("bônus e bonus", "bônus", "bonus")).containsExactly("bônus", "bonus");
    }

    @Test
    void mesmoTermoRepetidoNaListaNaoDuplica() {
        assertThat(trechos("garantia total", "garantia", "garantia")).containsExactly("garantia");
    }

    @Test
    void duasOcorrenciasComCaixasDiferentes() {
        assertThat(trechos("Lucro! lucro.", "lucro")).containsExactly("Lucro", "lucro");
    }

    // Regra 10: ordem
    @Test
    void ordenaPorInicioMesmoComTermosForaDeOrdem() {
        assertThat(buscador.buscar("bonus antes, garantia depois", List.of("garantia", "bonus")))
                .extracting(Ocorrencia::inicio).containsExactly(0, 13);
    }

    @Test
    void empateNoInicioPoeOMaiorPrimeiro() {
        assertThat(trechos("acesso vitalicio", "acesso", "acesso vitalicio"))
                .containsExactly("acesso vitalicio", "acesso");
    }

    // Regras 1 e 2: entradas vazias e termo com espaços
    @Test
    void textoNuloOuVazioDevolveVazio() {
        assertThat(buscador.buscar(null, List.of("x"))).isEmpty();
        assertThat(buscador.buscar("", List.of("x"))).isEmpty();
    }

    @Test
    void termosNulosOuVaziosDevolvemVazio() {
        assertThat(buscador.buscar("texto", List.of())).isEmpty();
        assertThat(buscador.buscar("texto", null)).isEmpty();
        assertThat(buscador.buscar("texto", List.of("  "))).isEmpty();
        assertThat(buscador.buscar("texto", Arrays.asList("texto", null))).hasSize(1);
    }

    @Test
    void termoEhTrimado() {
        assertThat(buscador.buscar("  garantia  ", List.of("  garantia ")))
                .containsExactly(new Ocorrencia("garantia", 2, 10, "garantia"));
    }

    // Regra 8: o trecho é sempre literal, em todos os casos
    static Stream<Arguments> todosOsCasos() {
        List<Arguments> casos = new ArrayList<>();
        casos.add(Arguments.of("Resultado GARANTIDO!", List.of("garantido")));
        casos.add(Arguments.of("ganhe já e ganhe ja", List.of("ganhe ja", "ganhe já")));
        casos.add(Arguments.of("Aprovação imediata", List.of("aprovacao")));
        casos.add(Arguments.of("renda   extra e renda extra", List.of("renda extra")));
        casos.add(Arguments.of("Satisfação 100% garantida no e-book (grátis)", List.of("100%", "e-book", "(grátis)")));
        casos.add(Arguments.of("lucro garantido", List.of("garantido", "lucro garantido")));
        casos.add(Arguments.of("Ação: ÇÃO ção açao", List.of("acao", "cao")));
        return casos.stream();
    }

    @ParameterizedTest
    @MethodSource("todosOsCasos")
    void trechoEhSempreOPedacoLiteralDoTexto(String texto, List<String> termos) {
        List<Ocorrencia> r = buscador.buscar(texto, termos);

        assertThat(r).isNotEmpty();
        r.forEach(o -> assertThat(o.trecho()).isEqualTo(texto.substring(o.inicio(), o.fim())));
    }
}
