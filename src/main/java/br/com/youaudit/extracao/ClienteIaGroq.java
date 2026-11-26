package br.com.youaudit.extracao;

import br.com.youaudit.config.GroqPropriedades;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
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
import java.util.concurrent.locks.ReentrantLock;

/**
 * Chamada ao Groq (API no formato da OpenAI: POST /chat/completions), com o RestClient do Spring (sem dependência nova).
 * Troca decidida em 26/11 porque a cota gratuita do Gemini (20 pedidos/dia) não dava para o projeto.
 *
 * <ul>
 *   <li>Resposta em JSON ESTRITO: {@code response_format: json_schema, strict: true}, com o esquema convertido por
 *       {@link ConversorEsquema}. O código valida tudo de novo mesmo assim (a IA nunca é confiável por padrão).</li>
 *   <li>{@code temperature: 0} e {@code reasoning_effort: low}: varia pouco e gasta poucos tokens pensando.</li>
 *   <li>A chave vai no cabeçalho {@code Authorization}, nunca na URL nem em log ou mensagem de erro.</li>
 *   <li>429 (limite por minuto): espera o que o cabeçalho Retry-After pede (com teto), senão espera crescente.
 *       Rede ou 5xx: espera crescente. Outros erros (400, 401, 403, 413) falham na hora.</li>
 *   <li>{@code finish_reason = length} (resposta cortada) é falha: um JSON pela metade não serve.</li>
 *   <li>UMA chamada por vez por modelo ({@link #fila}): com 3 auditorias em paralelo, as chamadas juntas estouravam o
 *       limite por minuto (visto em 26/11: 2 auditorias FALHARAM com 429). Em fila, cada uma espera a sua vez.</li>
 * </ul>
 */
public class ClienteIaGroq implements ClienteIa {

    private static final Logger log = LoggerFactory.getLogger(ClienteIaGroq.class);

    private final GroqPropriedades propriedades;
    private final String modelo;
    private final RestClient http;
    /** Justa (true): quem chegou primeiro é atendido primeiro. */
    private final ReentrantLock fila = new ReentrantLock(true);

    public ClienteIaGroq(GroqPropriedades propriedades, String modelo) {
        this.propriedades = propriedades;
        this.modelo = modelo;
        HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(propriedades.timeoutMs())).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(cliente);
        fabrica.setReadTimeout(Duration.ofMillis(propriedades.timeoutMs()));
        this.http = RestClient.builder().baseUrl(propriedades.baseUrl()).requestFactory(fabrica).build();
    }

    @Override
    public String modelo() {
        return modelo;
    }

    @Override
    public String gerarJson(PedidoIa pedido) {
        String chave = propriedades.apiKey();
        if (chave == null || chave.isBlank()) {
            throw new FalhaIaException("GROQ_API_KEY não definida: a chamada à IA não foi feita");
        }
        fila.lock();
        try {
            try {
                return chamar(chave, corpo(pedido, true));
            } catch (EsquemaRecusadoPeloGroq e) {
                // O modelo escreveu algo fora do esquema estrito e o PRÓPRIO Groq recusou (json_validate_failed).
                // Uma nova tentativa só pedindo "um JSON": o nosso analisador confere o esquema de qualquer jeito.
                log.warn("Groq ({}): resposta fora do esquema estrito; nova tentativa em modo JSON simples", modelo);
                return chamar(chave, corpo(pedido, false));
            }
        } finally {
            fila.unlock();
        }
    }

    private String chamar(String chave, Map<String, Object> corpo) {
        String ultimoMotivo = "";
        long proximaEspera = 0;
        for (int tentativa = 0; tentativa <= propriedades.tentativasExtras(); tentativa++) {
            if (tentativa > 0) {
                esperar(proximaEspera);
            }
            long esperaPadrao = Math.min(propriedades.esperaEntreTentativasMs() << tentativa, propriedades.esperaMaximaMs());
            try {
                JsonNode resposta = http.post()
                        .uri("/chat/completions")
                        .header("Authorization", "Bearer " + chave)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(semMarcaInterna(corpo))
                        .retrieve()
                        .body(JsonNode.class);
                return textoDaResposta(resposta);
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                if (status == 400 && Boolean.TRUE.equals(corpo.get(ESTRITO)) && codigoDoErro(e).equals(", json_validate_failed")) {
                    throw new EsquemaRecusadoPeloGroq();
                }
                if (status != 429 && !(e instanceof HttpServerErrorException)) {
                    throw new FalhaIaException("o Groq recusou o pedido (HTTP " + status + codigoDoErro(e) + ")");
                }
                ultimoMotivo = "HTTP " + status;
                proximaEspera = status == 429 ? esperaPedida(e, esperaPadrao) : esperaPadrao;
            } catch (ResourceAccessException e) {
                ultimoMotivo = "falha de rede ou tempo esgotado";
                proximaEspera = esperaPadrao;
            } catch (RestClientException e) {
                ultimoMotivo = "resposta ilegível";
                proximaEspera = esperaPadrao;
            }
            log.warn("Groq ({}): tentativa {} falhou ({})", modelo, tentativa + 1, ultimoMotivo);
        }
        throw new FalhaIaException("Groq indisponível depois de " + (propriedades.tentativasExtras() + 1)
                + " tentativas (" + ultimoMotivo + ")");
    }

    /** Marca interna (não vai para a API): o corpo pede o esquema estrito. */
    private static final String ESTRITO = "__estrito";

    /** Sinal interno: o Groq recusou a resposta por não seguir o esquema estrito. */
    private static final class EsquemaRecusadoPeloGroq extends RuntimeException {
    }

    private Map<String, Object> corpo(PedidoIa pedido, boolean estrito) {
        Map<String, Object> esquema = new LinkedHashMap<>();
        esquema.put("name", "resposta");
        esquema.put("strict", true);
        esquema.put("schema", ConversorEsquema.paraJsonSchema(pedido.esquemaResposta()));

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("model", modelo);
        corpo.put("temperature", 0);
        corpo.put("reasoning_effort", propriedades.reasoningEffort());
        corpo.put("max_completion_tokens", propriedades.maxTokensSaida());
        corpo.put("messages", List.of(
                Map.of("role", "system", "content", pedido.instrucaoSistema()),
                Map.of("role", "user", "content", pedido.conteudo())));
        corpo.put("response_format", estrito ? Map.of("type", "json_schema", "json_schema", esquema)
                : Map.of("type", "json_object"));
        corpo.put(ESTRITO, estrito);
        return corpo;
    }

    /** O texto fica em choices[0].message.content. Sem texto, ou cortado por tamanho, é falha. */
    private static String textoDaResposta(JsonNode resposta) {
        JsonNode escolha = resposta == null ? null : resposta.path("choices").path(0);
        String motivo = escolha == null ? "" : escolha.path("finish_reason").asText("");
        if ("length".equals(motivo)) {
            throw new FalhaIaException("a resposta do Groq foi cortada por tamanho (finish_reason=length)");
        }
        String texto = escolha == null ? "" : escolha.path("message").path("content").asText("");
        if (texto.isBlank()) {
            throw new FalhaIaException("o Groq não devolveu texto" + (motivo.isEmpty() ? "" : " (finish_reason=" + motivo + ")"));
        }
        return texto;
    }

    /**
     * Só o CÓDIGO do erro (ex.: "json_validate_failed"), nunca a mensagem inteira: ela pode repetir texto da página
     * ou da resposta. Chave nunca aparece aqui (não está no corpo da resposta).
     */
    private static String codigoDoErro(RestClientResponseException e) {
        try {
            String codigo = new com.fasterxml.jackson.databind.ObjectMapper().readTree(e.getResponseBodyAsString())
                    .path("error").path("code").asText("");
            return codigo.matches("[a-z_]{1,60}") ? ", " + codigo : "";
        } catch (Exception ignorar) {
            return "";
        }
    }

    private static Map<String, Object> semMarcaInterna(Map<String, Object> corpo) {
        Map<String, Object> copia = new LinkedHashMap<>(corpo);
        copia.remove(ESTRITO);
        return copia;
    }

    /** Retry-After vem em segundos (pode ter fração). Sem cabeçalho, usa a espera padrão. Sempre com teto. */
    private long esperaPedida(RestClientResponseException e, long padrao) {
        String valor = e.getResponseHeaders() == null ? null : e.getResponseHeaders().getFirst("retry-after");
        if (valor == null) {
            return padrao;
        }
        try {
            long ms = (long) Math.ceil(Double.parseDouble(valor.strip()) * 1000);
            return Math.min(Math.max(ms, 0), propriedades.esperaMaximaMs());
        } catch (NumberFormatException ex) {
            return padrao;
        }
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
