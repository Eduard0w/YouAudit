package br.com.youaudit.config;

import br.com.youaudit.baseline.TermosBaseline;
import br.com.youaudit.baseline.UnidadeQuantidade;
import br.com.youaudit.extracao.TipoAfirmacao;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BaselineConfigTest {

    @Test
    void carregaOArquivoDoProjeto() {
        TermosBaseline t = new BaselineConfig().termosBaseline(new ObjectMapper());

        assertThat(t.fonte()).containsIgnoringCase("hipótese de trabalho");
        assertThat(t.termosProibidos()).isNotEmpty();
        assertThat(t.palavrasChavePorTipo()).containsKey(TipoAfirmacao.GARANTIA);
        assertThat(t.unidades()).containsKeys(UnidadeQuantidade.values());
    }

    @Test
    void recusaTipoSemPalavraChave() {
        Map<TipoAfirmacao, List<String>> palavras = palavrasCompletas();
        palavras.remove(TipoAfirmacao.BONUS);

        assertThatThrownBy(() -> BaselineConfig.validar(termos(palavras, unidadesValidas(), List.of("x"))))
                .hasMessageContaining("palavrasChavePorTipo.BONUS");
    }

    @Test
    void recusaUnidadeComAcentoOuMaiuscula() {
        Map<UnidadeQuantidade, List<String>> unidades = unidadesValidas();
        unidades.put(UnidadeQuantidade.MODULO, List.of("módulos"));

        assertThatThrownBy(() -> BaselineConfig.validar(termos(palavrasCompletas(), unidades, List.of("x"))))
                .hasMessageContaining("módulos");
    }

    @Test
    void recusaListaVaziaOuComTermoEmBranco() {
        assertThatThrownBy(() -> BaselineConfig.validar(termos(palavrasCompletas(), unidadesValidas(), List.of())))
                .hasMessageContaining("termosProibidos");
        assertThatThrownBy(() -> BaselineConfig.validar(termos(palavrasCompletas(), unidadesValidas(), List.of(" "))))
                .hasMessageContaining("termosProibidos");
    }

    private static TermosBaseline termos(Map<TipoAfirmacao, List<String>> palavras,
                                         Map<UnidadeQuantidade, List<String>> unidades, List<String> proibidos) {
        return new TermosBaseline("teste", proibidos, palavras, List.of("resultados variam"), 1, List.of("pago"),
                Map.of("canal de contato", List.of("suporte")), unidades);
    }

    private static Map<TipoAfirmacao, List<String>> palavrasCompletas() {
        Map<TipoAfirmacao, List<String>> palavras = new EnumMap<>(TipoAfirmacao.class);
        for (TipoAfirmacao tipo : TipoAfirmacao.values()) {
            if (tipo != TipoAfirmacao.OUTRO) {
                palavras.put(tipo, List.of(tipo.name().toLowerCase()));
            }
        }
        return palavras;
    }

    private static Map<UnidadeQuantidade, List<String>> unidadesValidas() {
        Map<UnidadeQuantidade, List<String>> unidades = new EnumMap<>(UnidadeQuantidade.class);
        unidades.put(UnidadeQuantidade.AULA, List.of("aulas"));
        unidades.put(UnidadeQuantidade.MODULO, List.of("modulos"));
        return unidades;
    }
}
