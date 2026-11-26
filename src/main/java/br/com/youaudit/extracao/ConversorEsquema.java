package br.com.youaudit.extracao;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converte os esquemas do projeto (escritos no formato do Gemini: "OBJECT", "STRING", "nullable") para o JSON Schema
 * padrão no modo ESTRITO que o Groq usa ({@code response_format: json_schema, strict: true}).
 *
 * <p>O modo estrito exige: todo campo listado em {@code required} e {@code additionalProperties: false}. Campo que no
 * nosso esquema era opcional ou "nullable" vira {@code ["string", "null"]}: a IA é obrigada a mandar o campo, mas pode
 * mandar null. O nosso analisador já aceita null nesses campos, então o comportamento não muda.
 */
public final class ConversorEsquema {

    private ConversorEsquema() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> paraJsonSchema(Map<String, Object> gemini) {
        Map<String, Object> saida = new LinkedHashMap<>();
        String tipo = String.valueOf(gemini.get("type")).toLowerCase();
        boolean nulo = Boolean.TRUE.equals(gemini.get("nullable"));
        saida.put("type", nulo ? List.of(tipo, "null") : tipo);
        if (gemini.containsKey("enum")) {
            List<Object> valores = new ArrayList<>((List<Object>) gemini.get("enum"));
            if (nulo) {
                valores.add(null);
            }
            saida.put("enum", valores);
        }
        if (gemini.get("properties") instanceof Map<?, ?> propriedades) {
            Map<String, Object> convertidas = new LinkedHashMap<>();
            List<String> obrigatorias = new ArrayList<>();
            List<Object> antes = gemini.get("required") instanceof List<?> l ? (List<Object>) l : List.of();
            for (Map.Entry<?, ?> e : propriedades.entrySet()) {
                Map<String, Object> filho = new LinkedHashMap<>((Map<String, Object>) e.getValue());
                if (!antes.contains(e.getKey())) {
                    filho.put("nullable", true); // era opcional: no modo estrito vira "obrigatório, pode ser null"
                }
                convertidas.put((String) e.getKey(), paraJsonSchema(filho));
                obrigatorias.add((String) e.getKey());
            }
            saida.put("properties", convertidas);
            saida.put("required", obrigatorias);
            saida.put("additionalProperties", false);
        }
        if (gemini.get("items") instanceof Map<?, ?> itens) {
            saida.put("items", paraJsonSchema((Map<String, Object>) itens));
        }
        return saida;
    }
}
