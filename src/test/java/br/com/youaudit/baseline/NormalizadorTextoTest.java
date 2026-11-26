package br.com.youaudit.baseline;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NormalizadorTextoTest {

    @Test
    void tiraCaixaAcentoECedilha() {
        assertThat(NormalizadorTexto.normalizar("AÇÃO Módulo Já")).isEqualTo("acao modulo ja");
    }

    @Test
    void todoEspacoViraEspacoSimples() {
        assertThat(NormalizadorTexto.normalizar("a b\tc\nd")).isEqualTo("a b c d");
    }

    @Test
    void mantemOTamanho() {
        String texto = "Ação: ÇÃO 100% (grátis) 🍰 e-book ";
        assertThat(NormalizadorTexto.normalizar(texto)).hasSameSizeAs(texto);
    }
}
