package br.com.youaudit.extracao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class ValidadorTrechoLiteralTest {

    private static final String FONTE =
            "São mais de 40 aulas práticas. Bônus: mentoria. Apenas R$ 297";
    private final ValidadorTrechoLiteral validador = new ValidadorTrechoLiteral();

    @Test
    void trechoExatoEValido() {
        assertTrue(validador.validar("40 aulas práticas", FONTE).valido());
    }

    @Test
    void mudancaDeCaixaEInvalida() {
        var r = validador.validar("40 Aulas práticas", FONTE);
        assertFalse(r.valido());
        assertEquals("trecho não encontrado literalmente na fonte", r.motivo());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t"})
    void trechoVazioOuNuloEInvalido(String trecho) {
        var r = validador.validar(trecho, FONTE);
        assertFalse(r.valido());
        assertEquals("trecho vazio", r.motivo());
    }

    @Test
    void fonteInteiraComoTrechoEValida() {
        assertTrue(validador.validar(FONTE, FONTE).valido());
    }

    @ParameterizedTest(name = "trecho inválido: [{0}]")
    @ValueSource(strings = {
            "Bonus: mentoria",        // sem acento
            "R$297",                  // sem o espaço
            "Apenas R$ 297 ",         // espaço a mais no fim
            "mais de quarenta aulas"  // inventado
    })
    void trechosQueNaoAparecemLiteralmenteSaoInvalidos(String trecho) {
        var r = validador.validar(trecho, FONTE);
        assertFalse(r.valido());
        assertEquals("trecho não encontrado literalmente na fonte", r.motivo());
    }

    @Test
    void trechoDeOutroBlocoEInvalido() {
        String outroBloco = "Garantia de 7 dias para todos os alunos.";
        assertFalse(validador.validar("Garantia de 7 dias", FONTE).valido());
        assertTrue(validador.validar("Garantia de 7 dias", outroBloco).valido());
    }

    @Test
    void fonteNulaEInvalida() {
        var r = validador.validar("40 aulas práticas", null);
        assertFalse(r.valido());
        assertEquals("fonte inexistente", r.motivo());
    }
}
