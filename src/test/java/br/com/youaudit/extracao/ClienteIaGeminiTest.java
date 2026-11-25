package br.com.youaudit.extracao;

import br.com.youaudit.coleta.ServidorHttpLocal;
import br.com.youaudit.config.GeminiPropriedades;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** O "Gemini" aqui é um servidor HTTP local: nenhuma chamada sai da máquina e nenhuma chave real é usada. */
class ClienteIaGeminiTest {

    private static final String CHAVE_FALSA = "chave-falsa-de-teste-123";
    private static final String CAMINHO = "/v1beta/models/modelo-teste:generateContent";
    private static final String RESPOSTA_OK = """
            {"candidates": [{"content": {"parts": [{"text": "{\\"afirmacoes\\": []}"}]}, "finishReason": "STOP"}]}
            """;
    private static final PedidoIa PEDIDO = new PedidoIa("instrução de sistema", "[bloco 1]\n40 aulas",
            Map.of("type", "OBJECT"));

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
    void devolveOTextoDaResposta() {
        servidor.responder(CAMINHO, 200, "application/json", RESPOSTA_OK);

        assertThat(cliente(CHAVE_FALSA).gerarJson(PEDIDO)).isEqualTo("{\"afirmacoes\": []}");
    }

    @Test
    void chaveVaiNoCabecalhoENuncaNaUrl() {
        servidor.responder(CAMINHO, 200, "application/json", RESPOSTA_OK);

        cliente(CHAVE_FALSA).gerarJson(PEDIDO);

        assertThat(servidor.ultimoCabecalho("x-goog-api-key")).isEqualTo(CHAVE_FALSA);
        assertThat(servidor.pedidos()).containsExactly(CAMINHO);
    }

    @Test
    void corpoTemInstrucaoConteudoEModoJson() throws Exception {
        servidor.responder(CAMINHO, 200, "application/json", RESPOSTA_OK);

        cliente(CHAVE_FALSA).gerarJson(PEDIDO);

        JsonNode corpo = json.readTree(servidor.ultimoCorpo());
        assertThat(corpo.at("/systemInstruction/parts/0/text").asText()).isEqualTo("instrução de sistema");
        assertThat(corpo.at("/contents/0/parts/0/text").asText()).isEqualTo("[bloco 1]\n40 aulas");
        assertThat(corpo.at("/generationConfig/responseMimeType").asText()).isEqualTo("application/json");
        assertThat(corpo.at("/generationConfig/responseSchema/type").asText()).isEqualTo("OBJECT");
        assertThat(corpo.at("/generationConfig/temperature").asInt()).isZero();
    }

    @Test
    void semChaveFalhaSemChamarAApi() {
        assertThatThrownBy(() -> cliente("").gerarJson(PEDIDO))
                .isInstanceOf(FalhaIaException.class)
                .hasMessageContaining("GEMINI_API_KEY");
        assertThat(servidor.pedidos()).isEmpty();
    }

    @Test
    void erro500ETentadoDeNovoEDepoisFunciona() {
        servidor.emSequencia(CAMINHO, 500, "application/json", "{}")
                .responder(CAMINHO, 200, "application/json", RESPOSTA_OK);

        assertThat(cliente(CHAVE_FALSA).gerarJson(PEDIDO)).isEqualTo("{\"afirmacoes\": []}");
        assertThat(servidor.pedidos()).hasSize(2);
    }

    @Test
    void limite429ETentadoDuasVezesAMaisEDepoisFalha() {
        servidor.responder(CAMINHO, 429, "application/json", "{}");

        assertThatThrownBy(() -> cliente(CHAVE_FALSA).gerarJson(PEDIDO))
                .isInstanceOf(FalhaIaException.class)
                .hasMessageContaining("3 tentativas")
                .hasMessageContaining("HTTP 429");
        assertThat(servidor.pedidos()).hasSize(3);
    }

    @Test
    void erro400NaoETentadoDeNovo() {
        servidor.responder(CAMINHO, 400, "application/json", "{\"error\": {\"message\": \"pedido ruim\"}}");

        assertThatThrownBy(() -> cliente(CHAVE_FALSA).gerarJson(PEDIDO))
                .isInstanceOf(FalhaIaException.class)
                .hasMessageContaining("HTTP 400");
        assertThat(servidor.pedidos()).hasSize(1);
    }

    @Test
    void respostaSemTextoEhFalhaComOMotivo() {
        servidor.responder(CAMINHO, 200, "application/json", "{\"candidates\": [{\"finishReason\": \"SAFETY\"}]}");

        assertThatThrownBy(() -> cliente(CHAVE_FALSA).gerarJson(PEDIDO))
                .isInstanceOf(FalhaIaException.class)
                .hasMessageContaining("SAFETY");
    }

    @Test
    void mensagensDeErroNuncaContemAChave() {
        servidor.responder(CAMINHO, 401, "application/json", "{\"error\": \"" + CHAVE_FALSA + " inválida\"}");

        assertThatThrownBy(() -> cliente(CHAVE_FALSA).gerarJson(PEDIDO)).hasMessageNotContaining(CHAVE_FALSA);
    }

    @Test
    void toStringDaConfiguracaoNaoMostraAChave() {
        assertThat(propriedades(CHAVE_FALSA).toString()).doesNotContain(CHAVE_FALSA).contains("(definida)");
    }

    private ClienteIaGemini cliente(String chave) {
        return new ClienteIaGemini(propriedades(chave));
    }

    private GeminiPropriedades propriedades(String chave) {
        return new GeminiPropriedades(chave, "modelo-teste", servidor.url("/v1beta"), 2000, 2, 0);
    }
}
