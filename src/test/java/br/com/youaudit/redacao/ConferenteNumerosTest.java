package br.com.youaudit.redacao;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConferenteNumerosTest {

    private static final String DADOS = "São 40 aulas\nGarantia de 7 dias\nApenas R$ 1.297,00\nAula 12";
    private final ConferenteNumeros conferente = new ConferenteNumeros();

    @Test
    void numerosQueExistemNosDadosPassam() {
        assertThat(conferente.numerosInventados("Troque 40 aulas por 28? Mantenha a garantia de 7 dias.",
                DADOS + "\n28 aulas")).isEmpty();
    }

    @Test
    void numeroInventadoERecusado() {
        assertThat(conferente.numerosInventados("Ofereça garantia de 30 dias.", DADOS)).containsExactly("30");
    }

    @Test
    void numeroNaoCasaDentroDeOutroMaior() {
        // "12" existe ("Aula 12"), mas "1" sozinho não: "1" não pode casar dentro de "12" nem de "1.297"
        assertThat(conferente.numerosInventados("Veja a aula 1", DADOS)).containsExactly("1");
    }

    @Test
    void separadorDeMilharNaoAtrapalha() {
        assertThat(conferente.numerosInventados("O preço de R$ 1297 está correto.", DADOS)).isEmpty();
    }

    @Test
    void textoSemNumeroPassa() {
        assertThat(conferente.numerosInventados("Inclua um aviso de que resultados variam.", DADOS)).isEmpty();
    }
}
