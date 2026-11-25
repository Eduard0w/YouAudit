package br.com.youaudit.extracao;

import br.com.youaudit.config.RelogioConfig;
import br.com.youaudit.evento.CalculadoraHash;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({CacheRespostasIa.class, CalculadoraHash.class, RelogioConfig.class})
class CacheRespostasIaTest {

    private static final PedidoIa PEDIDO = new PedidoIa("sistema v1", "[bloco 1]\n40 aulas", Map.of());

    @Autowired private CacheRespostasIa cache;

    @Test
    void mesmaEntradaGeraMesmaChave() {
        assertThat(cache.chave("extracao", "v1", "modelo-a", PEDIDO)).isEqualTo(cache.chave("extracao", "v1", "modelo-a", PEDIDO));
    }

    @Test
    void chaveMudaComTarefaVersaoModeloInstrucaoOuConteudo() {
        String base = cache.chave("extracao", "v1", "modelo-a", PEDIDO);

        assertThat(cache.chave("cruzamento", "v1", "modelo-a", PEDIDO)).isNotEqualTo(base);
        assertThat(cache.chave("extracao", "v2", "modelo-a", PEDIDO)).isNotEqualTo(base);
        assertThat(cache.chave("extracao", "v1", "modelo-b", PEDIDO)).isNotEqualTo(base);
        assertThat(cache.chave("extracao", "v1", "modelo-a", new PedidoIa("sistema v1 alterado", PEDIDO.conteudo(), Map.of())))
                .isNotEqualTo(base);
        assertThat(cache.chave("extracao", "v1", "modelo-a", new PedidoIa(PEDIDO.instrucaoSistema(), "[bloco 1]\n28 aulas", Map.of())))
                .isNotEqualTo(base);
    }

    @Test
    void chaveTemFormatoTarefaVersaoModeloHash() {
        assertThat(cache.chave("extracao", "v1", "modelo-a", PEDIDO)).matches("extracao:v1:modelo-a:[0-9a-f]{64}");
    }

    @Test
    void guardaEDevolve() {
        String chave = cache.chave("extracao", "v1", "modelo-a", PEDIDO);

        assertThat(cache.buscar(chave)).isEmpty();
        cache.guardar(chave, "{\"afirmacoes\": []}");
        assertThat(cache.buscar(chave)).contains("{\"afirmacoes\": []}");
    }

    @Test
    void guardarDeNovoMantemAPrimeiraResposta() {
        String chave = cache.chave("extracao", "v1", "modelo-a", PEDIDO);

        cache.guardar(chave, "primeira");
        cache.guardar(chave, "segunda");

        assertThat(cache.buscar(chave)).contains("primeira");
    }
}
