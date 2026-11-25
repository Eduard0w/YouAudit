package br.com.youaudit.coleta;

import br.com.youaudit.config.ColetaPropriedades;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LimitadorRequisicoesTest {

    @Test
    void primeiraRequisicaoNaoEspera() {
        LimitadorRequisicoes limitador = new LimitadorRequisicoes(propriedades(1000));

        long inicio = System.nanoTime();
        limitador.aguardarVez();

        assertThat(ms(System.nanoTime() - inicio)).isLessThan(500);
    }

    @Test
    void segundaRequisicaoEsperaCompletarOIntervalo() {
        LimitadorRequisicoes limitador = new LimitadorRequisicoes(propriedades(1000));

        limitador.aguardarVez();
        long inicio = System.nanoTime();
        limitador.aguardarVez();

        assertThat(ms(System.nanoTime() - inicio)).isGreaterThanOrEqualTo(950);
    }

    private static long ms(long nanos) {
        return nanos / 1_000_000L;
    }

    private static ColetaPropriedades propriedades(long intervaloMs) {
        return new ColetaPropriedades("teste/0", 1000, intervaloMs, 500, Path.of("nao-usado"), null);
    }
}
