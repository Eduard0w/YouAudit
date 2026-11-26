package br.com.youaudit.pontuacao;

import br.com.youaudit.config.PontuacaoPropriedades;
import br.com.youaudit.regras.TipoAchado;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static br.com.youaudit.regras.TipoAchado.CUSTO_OMITIDO;
import static br.com.youaudit.regras.TipoAchado.ELEMENTO_AUSENTE_NA_ENTREGA;
import static br.com.youaudit.regras.TipoAchado.INFO_ESSENCIAL_AUSENTE;
import static br.com.youaudit.regras.TipoAchado.PROMESSA_RESULTADO_SEM_RESSALVA;
import static br.com.youaudit.regras.TipoAchado.QUANTIDADE_DIVERGENTE;
import static br.com.youaudit.regras.TipoAchado.RECONHECIMENTO_SEM_COMPROVANTE;
import static br.com.youaudit.regras.TipoAchado.TERMO_PROIBIDO;
import static org.assertj.core.api.Assertions.assertThat;

/** Pesos e faixas do CONTRATOS.md §2 (os mesmos do data.sql). */
class CalculadoraPontuacaoTest {

    private static final Map<TipoAchado, Integer> PESOS = new EnumMap<>(Map.of(
            TERMO_PROIBIDO, 30, PROMESSA_RESULTADO_SEM_RESSALVA, 20, RECONHECIMENTO_SEM_COMPROVANTE, 25,
            QUANTIDADE_DIVERGENTE, 15, ELEMENTO_AUSENTE_NA_ENTREGA, 25, CUSTO_OMITIDO, 30, INFO_ESSENCIAL_AUSENTE, 10));

    private final CalculadoraPontuacao calculadora = new CalculadoraPontuacao(new PontuacaoPropriedades(30, 60, 100));

    @Test
    void exemploLojaLucrativaDa90EAlta() {
        ResultadoPontuacao r = calculadora.calcular(List.of(PROMESSA_RESULTADO_SEM_RESSALVA, QUANTIDADE_DIVERGENTE,
                ELEMENTO_AUSENTE_NA_ENTREGA, CUSTO_OMITIDO), PESOS);

        assertThat(r.pontos()).isEqualTo(90);
        assertThat(r.faixa()).isEqualTo(Faixa.ALTA);
        assertThat(r.parcelas()).extracting(ResultadoPontuacao.Parcela::peso).containsExactly(20, 15, 25, 30);
    }

    @Test
    void mesmoTipoRepetidoContaUmaVez() {
        ResultadoPontuacao r = calculadora.calcular(List.of(TERMO_PROIBIDO, TERMO_PROIBIDO, TERMO_PROIBIDO), PESOS);
        assertThat(r.pontos()).isEqualTo(30);
    }

    @Test
    void somaAcimaDoMaximoFicaEmCemEGuardaASomaOriginal() {
        ResultadoPontuacao r = calculadora.calcular(List.of(TipoAchado.values()), PESOS);
        assertThat(r.pontos()).isEqualTo(100);
        assertThat(r.somaSemTeto()).isEqualTo(155);
    }

    @Test
    void semAchadoEZeroEBaixa() {
        ResultadoPontuacao r = calculadora.calcular(List.of(), PESOS);
        assertThat(r.pontos()).isZero();
        assertThat(r.faixa()).isEqualTo(Faixa.BAIXA);
    }

    @Test
    void regraInativaValeZero() {
        Map<TipoAchado, Integer> semTermo = new EnumMap<>(PESOS);
        semTermo.remove(TERMO_PROIBIDO);
        assertThat(calculadora.calcular(List.of(TERMO_PROIBIDO), semTermo).pontos()).isZero();
    }

    @Test
    void limitesDasFaixas() {
        assertThat(calculadora.faixa(29)).isEqualTo(Faixa.BAIXA);
        assertThat(calculadora.faixa(30)).isEqualTo(Faixa.MEDIA);
        assertThat(calculadora.faixa(59)).isEqualTo(Faixa.MEDIA);
        assertThat(calculadora.faixa(60)).isEqualTo(Faixa.ALTA);
        assertThat(calculadora.faixa(100)).isEqualTo(Faixa.ALTA);
    }
}
