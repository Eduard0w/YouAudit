package br.com.youaudit.coleta;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LimpadorTextoTest {

    private final LimpadorTexto limpador = new LimpadorTexto();

    @Test
    void removeTagsEMantemUmaLinhaPorParagrafo() {
        String html = "<h1>Método Loja Lucrativa</h1><p>São <b>40 aulas</b> práticas.</p><p>Garantia de 7 dias</p>";

        assertThat(limpador.limpar(html))
                .isEqualTo("Método Loja Lucrativa\nSão 40 aulas práticas.\nGarantia de 7 dias");
    }

    @Test
    void descartaScriptEStyle() {
        String html = "<style>p{color:red}</style><p>Texto</p><script>alert('x')</script>";

        assertThat(limpador.limpar(html)).isEqualTo("Texto");
    }

    @Test
    void brViraQuebraDeLinha() {
        assertThat(limpador.limpar("Linha um<br>Linha dois")).isEqualTo("Linha um\nLinha dois");
    }

    @Test
    void colapsaEspacosTabsENbspRepetidos() {
        assertThat(limpador.limpar("Apenas   R$\t\t297&nbsp;&nbsp;à vista")).isEqualTo("Apenas R$ 297 à vista");
    }

    @Test
    void textoPuroMantemQuebrasERemoveLinhasVazias() {
        String txt = "  Primeiro parágrafo  \r\n\r\n\n   Segundo parágrafo\n";

        assertThat(limpador.limpar(txt)).isEqualTo("Primeiro parágrafo\nSegundo parágrafo");
    }

    @Test
    void decodificaEntidadesHtml() {
        assertThat(limpador.limpar("<p>Bônus &amp; mentoria &quot;VIP&quot;</p>")).isEqualTo("Bônus & mentoria \"VIP\"");
    }

    @Test
    void naoMudaCaixaDasLetras() {
        assertThat(limpador.limpar("GARANTIA de 7 Dias")).isEqualTo("GARANTIA de 7 Dias");
    }

    @Test
    void nuloOuSoTagsViraTextoVazio() {
        assertThat(limpador.limpar(null)).isEmpty();
        assertThat(limpador.limpar("<div><p> </p></div>")).isEmpty();
    }
}
