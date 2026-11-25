package br.com.youaudit.evento;

import br.com.youaudit.coleta.LimpadorTexto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraHashTest {

    private final CalculadoraHash hash = new CalculadoraHash();
    private final LimpadorTexto limpador = new LimpadorTexto();

    @Test
    void bateComValorConhecidoDoSha256() {
        // Vetor de teste oficial do SHA-256 (FIPS 180-2): "abc".
        assertThat(hash.sha256("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void mesmoTextoDaMesmoHash() {
        assertThat(hash.sha256("40 aulas")).isEqualTo(hash.sha256("40 aulas"));
    }

    @Test
    void mudarCaixaDasLetrasMudaOHash() {
        assertThat(hash.sha256("Garantia de 7 dias")).isNotEqualTo(hash.sha256("garantia de 7 dias"));
    }

    @Test
    void mesmaPaginaComHtmlOuEspacosDiferentesDaMesmoHashDepoisDeLimpar() {
        String html = "<p>Apenas   <b>R$ 297</b></p><p>Garantia de 7 dias</p>";
        String texto = "Apenas R$ 297\n\n   Garantia de 7 dias  ";

        assertThat(hash.sha256(limpador.limpar(html))).isEqualTo(hash.sha256(limpador.limpar(texto)));
    }

    @Test
    void hashTem64CaracteresHexadecimais() {
        assertThat(hash.sha256("")).hasSize(64).matches("[0-9a-f]{64}");
    }
}
