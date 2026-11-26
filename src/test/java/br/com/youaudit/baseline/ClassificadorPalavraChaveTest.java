package br.com.youaudit.baseline;

import br.com.youaudit.avaliacao.AfirmacaoDetectada;
import br.com.youaudit.extracao.Bloco;
import br.com.youaudit.extracao.TipoAfirmacao;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ClassificadorPalavraChaveTest {

    private static ClassificadorPalavraChave classificador(Map<TipoAfirmacao, List<String>> palavras) {
        return new ClassificadorPalavraChave(palavras, new BuscadorTermosFake());
    }

    @Test
    void fraseVaiDoFimDaFraseAnteriorAteOProximo() {
        String texto = "Bem-vindo! Sao 40 aulas gravadas. Garantia de 7 dias.";
        int inicio = texto.indexOf("aulas");

        assertThat(ClassificadorPalavraChave.frase(texto, inicio, inicio + 5)).isEqualTo("Sao 40 aulas gravadas.");
    }

    @Test
    void pontoDentroDeNumeroNaoCortaAFrase() {
        String texto = "Fature R$ 1.200 por mes com o metodo";
        int inicio = texto.indexOf("por mes");

        assertThat(ClassificadorPalavraChave.frase(texto, inicio, inicio + 7)).isEqualTo(texto);
    }

    @Test
    void fraseSemPontuacaoEhOBlocoInteiroSemEspacosDasPontas() {
        assertThat(ClassificadorPalavraChave.frase("  acesso vitalicio  ", 2, 8)).isEqualTo("acesso vitalicio");
    }

    @Test
    void fraseEhSempreLiteralDoBloco() {
        String texto = "Ganhe! Muito?  Lucro rapido. Fim";
        for (int i = 0; i < texto.length(); i++) {
            assertThat(texto).contains(ClassificadorPalavraChave.frase(texto, i, i + 1));
        }
    }

    @Test
    void classificaPorTipoComBlocoEFrase() {
        Map<TipoAfirmacao, List<String>> palavras = new LinkedHashMap<>();
        palavras.put(TipoAfirmacao.QUANTIDADE_CONTEUDO, List.of("aulas"));
        palavras.put(TipoAfirmacao.GARANTIA, List.of("garantia"));
        List<Bloco> blocos = List.of(new Bloco(1, "Curso completo"), new Bloco(2, "Sao 40 aulas. Garantia de 7 dias."));

        assertThat(classificador(palavras).classificar(blocos)).containsExactly(
                new AfirmacaoDetectada(2, TipoAfirmacao.QUANTIDADE_CONTEUDO, "Sao 40 aulas."),
                new AfirmacaoDetectada(2, TipoAfirmacao.GARANTIA, "Garantia de 7 dias."));
    }

    @Test
    void mesmaFraseMesmoTipoPorDuasPalavrasContaUmaVez() {
        Map<TipoAfirmacao, List<String>> palavras = Map.of(TipoAfirmacao.GARANTIA, List.of("garantia", "reembolso"));
        List<Bloco> blocos = List.of(new Bloco(1, "Garantia com reembolso total."));

        assertThat(classificador(palavras).classificar(blocos)).hasSize(1);
    }

    @Test
    void mesmaFrasePodeTerDoisTipos() {
        Map<TipoAfirmacao, List<String>> palavras = new LinkedHashMap<>();
        palavras.put(TipoAfirmacao.RESULTADO_FINANCEIRO, List.of("faturar"));
        palavras.put(TipoAfirmacao.PRAZO_RESULTADO, List.of("em 30 dias"));
        List<Bloco> blocos = List.of(new Bloco(1, "Comece a faturar em 30 dias."));

        assertThat(classificador(palavras).classificar(blocos)).extracting(AfirmacaoDetectada::tipo)
                .containsExactly(TipoAfirmacao.RESULTADO_FINANCEIRO, TipoAfirmacao.PRAZO_RESULTADO);
    }
}
