package br.com.youaudit.extracao;

import br.com.youaudit.config.GeminiPropriedades;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Chamada real ao Gemini (REST generateContent), com o RestClient do próprio Spring (sem dependência nova).
 *
 * <ul>
 *   <li>Modo JSON: {@code responseMimeType: application/json} + {@code responseSchema}; {@code temperature: 0}
 *       para a resposta variar o mínimo possível entre execuções.</li>
 *   <li>A chave vai no cabeçalho {@code x-goog-api-key}, nunca na URL: URL aparece em log e em mensagem de erro.</li>
 *   <li>Erro de rede, 429 (limite) ou 5xx: novas tentativas com espera crescente. Outros erros (400, 401, 403)
 *       não melhoram tentando de novo: falha na hora.</li>
 * </ul>
 */
@Component
public class ClienteIaGemini implements ClienteIa {

    private static final Logger log = LoggerFactory.getLogger(ClienteIaGemini.class);

    private final GeminiPropriedades propriedades;
    private final RestClient http;

    public ClienteIaGemini(GeminiPropriedades propriedades) {
        this.propriedades = propriedades;
        HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(propriedades.timeoutMs())).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(cliente);
        fabrica.setReadTimeout(Duration.ofMillis(propriedades.timeoutMs()));
        this.http = RestClient.builder().baseUrl(propriedades.baseUrl()).requestFactory(fabrica).build();
    }

    @Override
    public String gerarJson(PedidoIa pedido) {
        String chave = propriedades.apiKey();
        if (chave == null || chave.isBlank()) {
            throw new FalhaIaException("GEMINI_API_KEY não definida: a chamada à IA não foi feita");
        }
        Map<String, Object> corpo = corpo(pedido);

        String ultimoMotivo = "";
        for (int tentativa = 0; tentativa <= propriedades.tentativasExtras(); tentativa++) {
            if (tentativa > 0) {
                esperar(propriedades.esperaEntreTentativasMs() << (tentativa - 1)); // 1 s, 2 s, 4 s...
            }
            try {
                JsonNode resposta = http.post()
                        .uri("/models/{modelo}:generateContent", propriedades.model())
                        .header("x-goog-api-key", chave)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(corpo)
                        .retrieve()
                        .body(JsonNode.class);
                return textoDaResposta(resposta);
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                if (status != 429 && !(e instanceof HttpServerErrorException)) {
                    throw new FalhaIaException("o Gemini recusou o pedido (HTTP " + status + ")");
                }
                ultimoMotivo = "HTTP " + status;
            } catch (ResourceAccessException e) {
                ultimoMotivo = "falha de rede ou tempo esgotado";
            } catch (RestClientException e) {
                ultimoMotivo = "resposta ilegível";
            }
            log.warn("Gemini: tentativa {} falhou ({})", tentativa + 1, ultimoMotivo);
        }
        throw new FalhaIaException("Gemini indisponível depois de " + (propriedades.tentativasExtras() + 1)
                + " tentativas (" + ultimoMotivo + ")");
    }

    @Override
    public String modelo() {
        return propriedades.model();
    }

    private static Map<String, Object> corpo(PedidoIa pedido) {
        Map<String, Object> configuracao = new LinkedHashMap<>();
        configuracao.put("responseMimeType", "application/json");
        configuracao.put("responseSchema", pedido.esquemaResposta());
        configuracao.put("temperature", 0);

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("systemInstruction", Map.of("parts", List.of(Map.of("text", pedido.instrucaoSistema()))));
        corpo.put("contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", pedido.conteudo())))));
        corpo.put("generationConfig", configuracao);
        return corpo;
    }

    /** O texto fica em candidates[0].content.parts[*].text. Sem texto (ex.: bloqueio de segurança) é falha. */
    private static String textoDaResposta(JsonNode resposta) {
        JsonNode candidato = resposta == null ? null : resposta.path("candidates").path(0);
        StringBuilder texto = new StringBuilder();
        if (candidato != null) {
            for (JsonNode parte : candidato.path("content").path("parts")) {
                texto.append(parte.path("text").asText(""));
            }
        }
        if (texto.isEmpty()) {
            String motivo = candidato == null ? "" : candidato.path("finishReason").asText("");
            throw new FalhaIaException("o Gemini não devolveu texto" + (motivo.isEmpty() ? "" : " (finishReason=" + motivo + ")"));
        }
        return texto.toString();
    }

    private static void esperar(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FalhaIaException("chamada à IA interrompida");
        }
    }
}
