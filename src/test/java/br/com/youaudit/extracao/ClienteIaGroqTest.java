package br.com.youaudit.extracao;

import br.com.youaudit.coleta.ServidorHttpLocal;
import br.com.youaudit.config.GroqPropriedades;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** O "Groq" aqui é um servidor HTTP local: nenhuma chamada sai da máquina e nenhuma chave real é usada. */
class ClienteIaGroqTest {

    private static final String CHAVE_FALSA = "chave-falsa-de-teste-456";
    private static final String CAMINHO = "/openai/v1/chat/completions";
    private static final String RESPOSTA_OK = """
            {"choices":[{"message":{"role":"assistant","content":"{\\"afirmacoes\\": []}"},"finish_reason":"stop"}]}""";
    private static final PedidoIa PEDIDO = new PedidoIa("instrução de sistema", "[bloco 1]\n40 aulas",
            PromptExtracao.esquema());

    private ServidorHttpLocal servidor;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void iniciar() {
        servidor = ServidorHttpLocal.iniciar();
    }

    @AfterEach
    void desligar() {
        servidor.close();
    }

    @Test
    void devolveOConteudoDaMensagem() {
        servidor.responder(CAMINHO, 200, "application/json", RESPOSTA_OK);
        assertThat(cliente(CHAVE_FALSA).gerarJson(PEDIDO)).isEqualTo("{\"afirmacoes\": []}");
    }

    @Test
    void chaveVaiNoCabecalhoAuthorizationENuncaNaUrl() {
        servidor.responder(CAMINHO, 200, "application/json", RESPOSTA_OK);
        cliente(CHAVE_FALSA).gerarJson(PEDIDO);

        assertThat(servidor.ultimoCabecalho("Authorization")).isEqualTo("Bearer " + CHAVE_FALSA);
        assertThat(servidor.pedidos()).containsExactly(CAMINHO);
    }

    @Test
    void corpoTemModeloMensagensEJsonEstrito() throws Exception {
        servidor.responder(CAMINHO, 200, "application/json", RESPOSTA_OK);
        cliente(CHAVE_FALSA).gerarJson(PEDIDO);

        JsonNode corpo = json.readTree(servidor.ultimoCorpo());
        assertThat(corpo.path("model").asText()).isEqualTo("modelo-teste");
        assertThat(corpo.path("temperature").asInt()).isZero();
        assertThat(corpo.path("messages").get(0).path("role").asText()).isEqualTo("system");
        assertThat(corpo.path("messages").get(0).path("content").asText()).isEqualTo("instrução de sistema");
        assertThat(corpo.path("messages").get(1).path("content").asText()).isEqualTo("[bloco 1]\n40 aulas");
        assertThat(corpo.path("response_format").path("type").asText()).isEqualTo("json_schema");
        assertThat(corpo.path("response_format").path("json_schema").path("strict").asBoolean()).isTrue();
        assertThat(corpo.path("response_format").path("json_schema").path("schema").path("type").asText()).isEqualTo("object");
    }

    @Test
    void semChaveFalhaSemChamar() {
        assertThatThrownBy(() -> cliente("").gerarJson(PEDIDO)).isInstanceOf(FalhaIaException.class)
                .hasMessageContaining("GROQ_API_KEY");
        assertThat(servidor.pedidos()).isEmpty();
    }

    @Test
    void limiteTemNovaTentativaEDepoisFunciona() {
        servidor.emSequencia(CAMINHO, 429, "application/json", "{\"error\":{\"message\":\"rate limit\"}}")
                .emSequencia(CAMINHO, 200, "application/json", RESPOSTA_OK);

        assertThat(cliente(CHAVE_FALSA).gerarJson(PEDIDO)).isEqualTo("{\"afirmacoes\": []}");
        assertThat(servidor.pedidos()).hasSize(2);
    }

    @Test
    void erro400FalhaNaHoraEAMensagemNaoTemAChave() {
        servidor.responder(CAMINHO, 400, "application/json", "{\"error\":{\"message\":\"bad\"}}");

        assertThatThrownBy(() -> cliente(CHAVE_FALSA).gerarJson(PEDIDO)).isInstanceOf(FalhaIaException.class)
                .hasMessageContaining("HTTP 400").message().doesNotContain(CHAVE_FALSA);
        assertThat(servidor.pedidos()).hasSize(1);
    }

    @Test
    void esquemaRecusadoPeloGroqTentaUmaVezEmModoJsonSimples() throws Exception {
        servidor.emSequencia(CAMINHO, 400, "application/json", "{\"error\":{\"code\":\"json_validate_failed\"}}")
                .emSequencia(CAMINHO, 200, "application/json", RESPOSTA_OK);

        assertThat(cliente(CHAVE_FALSA).gerarJson(PEDIDO)).isEqualTo("{\"afirmacoes\": []}");
        JsonNode segundo = json.readTree(servidor.ultimoCorpo());
        assertThat(segundo.path("response_format").path("type").asText()).isEqualTo("json_object");
        assertThat(segundo.has("__estrito")).isFalse(); // a marca interna não vai para a API
    }

    @Test
    void respostaCortadaPorTamanhoEFalha() {
        servidor.responder(CAMINHO, 200, "application/json", """
                {"choices":[{"message":{"content":"{\\"afirmacoes\\": [{"},"finish_reason":"length"}]}""");

        assertThatThrownBy(() -> cliente(CHAVE_FALSA).gerarJson(PEDIDO)).hasMessageContaining("cortada");
    }

    @Test
    void propriedadesNaoMostramAChaveNoToString() {
        assertThat(propriedades(CHAVE_FALSA).toString()).doesNotContain(CHAVE_FALSA);
    }

    @Test
    void conversorDeixaTudoObrigatorioEOpcionalViraNulavel() {
        Map<String, Object> s = ConversorEsquema.paraJsonSchema(PromptExtracao.esquema());
        @SuppressWarnings("unchecked")
        Map<String, Object> item = (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) s.get("properties"))
                .get("afirmacoes")).get("items");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) item.get("properties");

        assertThat(item.get("required")).isEqualTo(List.of("bloco_id", "tipo", "trecho", "valor", "confianca"));
        assertThat(item.get("additionalProperties")).isEqualTo(false);
        assertThat(((Map<?, ?>) props.get("valor")).get("type")).isEqualTo(List.of("string", "null"));
        assertThat(((Map<?, ?>) props.get("bloco_id")).get("type")).isEqualTo("integer");
        assertThat(((Map<?, ?>) props.get("tipo")).get("enum")).asList().contains("BONUS");
    }

    private ClienteIaGroq cliente(String chave) {
        return new ClienteIaGroq(propriedades(chave), "modelo-teste");
    }

    private GroqPropriedades propriedades(String chave) {
        return new GroqPropriedades(chave, "modelo-teste", "modelo-secundario", servidor.url("/openai/v1"),
                2000, 2, 0, 0, "low", 1000);
    }
}
