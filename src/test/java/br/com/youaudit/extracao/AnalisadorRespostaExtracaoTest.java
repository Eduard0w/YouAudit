package br.com.youaudit.extracao;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalisadorRespostaExtracaoTest {

    private static final List<Bloco> GRUPO = List.of(
            new Bloco(1, "Método Loja Lucrativa"),
            new Bloco(2, "São mais de 40 aulas práticas"),
            new Bloco(3, "Garantia de 7 dias"));

    private final AnalisadorRespostaExtracao analisador = new AnalisadorRespostaExtracao(new ValidadorTrechoLiteral());

    @Test
    void aceitaAfirmacaoComTrechoLiteralDoBlocoIndicado() {
        AnaliseResposta a = analisar(item(2, "QUANTIDADE_CONTEUDO", "40 aulas práticas", "\"40\"", "0.9"));

        assertThat(a.totalmenteValida()).isTrue();
        assertThat(a.aceitas()).containsExactly(
                new AnaliseResposta.Candidata(2, TipoAfirmacao.QUANTIDADE_CONTEUDO, "40 aulas práticas", "40", 0.9));
    }

    @Test
    void trechoInventadoEhRecusadoComOMotivoDoValidador() {
        AnaliseResposta a = analisar(item(2, "QUANTIDADE_CONTEUDO", "mais de quarenta aulas", "null", "0.9"));

        assertThat(a.aceitas()).isEmpty();
        assertThat(a.problemas()).extracting(AnaliseResposta.Problema::motivo)
                .containsExactly("trecho não encontrado literalmente na fonte");
    }

    @Test
    void trechoQueExisteEmOutroBlocoEhRecusado() {
        // "Garantia de 7 dias" existe no bloco 3, mas a IA disse bloco 2.
        AnaliseResposta a = analisar(item(2, "GARANTIA", "Garantia de 7 dias", "\"7 dias\"", "0.8"));

        assertThat(a.aceitas()).isEmpty();
        assertThat(a.problemas()).hasSize(1);
    }

    @Test
    void tipoForaDaListaEhRecusado() {
        AnaliseResposta a = analisar(item(3, "APROVADO", "Garantia de 7 dias", "null", "0.8"));

        assertThat(a.problemas()).extracting(AnaliseResposta.Problema::motivo).containsExactly("tipo fora da enumeração");
    }

    @Test
    void blocoInexistenteEhRecusado() {
        AnaliseResposta a = analisar(item(9, "GARANTIA", "Garantia de 7 dias", "null", "0.8"));

        assertThat(a.problemas()).extracting(AnaliseResposta.Problema::motivo)
                .containsExactly("bloco_id inexistente entre os blocos enviados");
    }

    @Test
    void confiancaForaDoIntervaloOuAusenteEhRecusada() {
        assertThat(analisar(item(3, "GARANTIA", "Garantia de 7 dias", "null", "1.5")).problemas()).hasSize(1);
        assertThat(analisar(item(3, "GARANTIA", "Garantia de 7 dias", "null", "\"alta\"")).problemas()).hasSize(1);
    }

    @Test
    void jsonInvalidoOuSemListaEhRespostaIlegivel() {
        assertThat(analisador.analisar("isto não é json", GRUPO).problemas())
                .extracting(AnaliseResposta.Problema::motivo).containsExactly("a resposta não é um JSON válido");
        assertThat(analisador.analisar("{\"outra\": []}", GRUPO).problemas())
                .extracting(AnaliseResposta.Problema::motivo).first().asString().contains("afirmacoes");
        assertThat(analisador.analisar(null, GRUPO).totalmenteValida()).isFalse();
    }

    @Test
    void listaVaziaEhValida() {
        AnaliseResposta a = analisador.analisar("{\"afirmacoes\": []}", GRUPO);

        assertThat(a.totalmenteValida()).isTrue();
        assertThat(a.aceitas()).isEmpty();
    }

    @Test
    void separaAsValidasDasInvalidasNaMesmaResposta() {
        AnaliseResposta a = analisar(
                item(3, "GARANTIA", "Garantia de 7 dias", "\"7 dias\"", "0.9"),
                item(2, "QUANTIDADE_CONTEUDO", "inventado", "null", "0.5"));

        assertThat(a.aceitas()).hasSize(1);
        assertThat(a.problemas()).hasSize(1);
    }

    @Test
    void afirmacaoRepetidaContaUmaVez() {
        String mesma = item(3, "GARANTIA", "Garantia de 7 dias", "\"7 dias\"", "0.9");

        assertThat(analisar(mesma, mesma).aceitas()).hasSize(1);
    }

    @Test
    void valorNumericoViraTexto() {
        AnaliseResposta a = analisar(item(2, "QUANTIDADE_CONTEUDO", "40 aulas", "40", "0.9"));

        assertThat(a.aceitas().get(0).valor()).isEqualTo("40");
    }

    private AnaliseResposta analisar(String... itens) {
        return analisador.analisar("{\"afirmacoes\": [" + String.join(",", itens) + "]}", GRUPO);
    }

    static String item(int bloco, String tipo, String trecho, String valorJson, String confiancaJson) {
        return "{\"bloco_id\": " + bloco + ", \"tipo\": \"" + tipo + "\", \"trecho\": \"" + trecho
                + "\", \"valor\": " + valorJson + ", \"confianca\": " + confiancaJson + "}";
    }
}
