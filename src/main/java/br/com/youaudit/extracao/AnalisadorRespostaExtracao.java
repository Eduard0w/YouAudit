package br.com.youaudit.extracao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Passo 5 do pipeline: confere a resposta da IA ANTES de qualquer uso (regra do CLAUDE.md).
 * Nada aqui confia na IA: o esquema pedido ao Gemini ajuda, mas é conferido de novo.
 *
 * <p>Por afirmação, nesta ordem: é um objeto; {@code bloco_id} existe no grupo enviado; {@code tipo} é um dos 14;
 * {@code confianca} é número de 0 a 1; e o {@code trecho} existe literalmente NO TEXTO DAQUELE BLOCO
 * ({@link ValidadorTrechoLiteral}). Afirmações repetidas (mesmo bloco, tipo e trecho) contam uma vez.
 */
@Component
public class AnalisadorRespostaExtracao {

    private final ValidadorTrechoLiteral validador;
    private final ObjectMapper json = new ObjectMapper();

    public AnalisadorRespostaExtracao(ValidadorTrechoLiteral validador) {
        this.validador = validador;
    }

    public AnaliseResposta analisar(String respostaJson, List<Bloco> grupo) {
        JsonNode raiz;
        try {
            raiz = json.readTree(respostaJson == null ? "" : respostaJson);
        } catch (JsonProcessingException e) {
            return ilegivel("a resposta não é um JSON válido");
        }
        JsonNode lista = raiz == null ? null : raiz.get("afirmacoes");
        if (lista == null || !lista.isArray()) {
            return ilegivel("a resposta não segue o esquema: falta a lista \"afirmacoes\"");
        }

        Map<Integer, Bloco> blocos = grupo.stream().collect(Collectors.toMap(Bloco::id, Function.identity()));
        List<AnaliseResposta.Candidata> aceitas = new ArrayList<>();
        List<AnaliseResposta.Problema> problemas = new ArrayList<>();
        Set<String> vistas = new HashSet<>();

        for (JsonNode item : lista) {
            if (!item.isObject()) {
                problemas.add(new AnaliseResposta.Problema(null, null, null, "item da lista não é um objeto"));
                continue;
            }
            Integer blocoId = item.path("bloco_id").canConvertToInt() && item.path("bloco_id").isIntegralNumber()
                    ? item.path("bloco_id").intValue() : null;
            String tipoTexto = texto(item.get("tipo"));
            String trecho = texto(item.get("trecho"));
            String motivo = motivoDeRecusa(item, blocoId, tipoTexto, trecho, blocos);
            if (motivo != null) {
                problemas.add(new AnaliseResposta.Problema(blocoId, tipoTexto, trecho, motivo));
                continue;
            }
            if (vistas.add(blocoId + "\u0000" + tipoTexto + "\u0000" + trecho)) {
                aceitas.add(new AnaliseResposta.Candidata(blocoId, TipoAfirmacao.valueOf(tipoTexto), trecho,
                        texto(item.get("valor")), item.get("confianca").doubleValue()));
            }
        }
        return new AnaliseResposta(aceitas, problemas);
    }

    /** Devolve o motivo da recusa, ou null se a afirmação passou em tudo. */
    private String motivoDeRecusa(JsonNode item, Integer blocoId, String tipo, String trecho, Map<Integer, Bloco> blocos) {
        if (blocoId == null || !blocos.containsKey(blocoId)) {
            return "bloco_id inexistente entre os blocos enviados";
        }
        if (tipo == null || !tipoValido(tipo)) {
            return "tipo fora da enumeração";
        }
        JsonNode confianca = item.get("confianca");
        if (confianca == null || !confianca.isNumber() || confianca.doubleValue() < 0 || confianca.doubleValue() > 1) {
            return "confiança ausente ou fora de 0 a 1";
        }
        ResultadoValidacao literal = validador.validar(trecho, blocos.get(blocoId).texto());
        return literal.valido() ? null : literal.motivo();
    }

    private static boolean tipoValido(String tipo) {
        for (TipoAfirmacao t : TipoAfirmacao.values()) {
            if (t.name().equals(tipo)) {
                return true;
            }
        }
        return false;
    }

    /** Texto de um campo; null se ausente ou nulo. Número vira texto ("40"), para o campo valor. */
    private static String texto(JsonNode no) {
        return no == null || no.isNull() ? null : no.asText();
    }

    private static AnaliseResposta ilegivel(String motivo) {
        return new AnaliseResposta(List.of(), List.of(new AnaliseResposta.Problema(null, null, null, motivo)));
    }
}
