package br.com.youaudit.entrega;

import br.com.youaudit.avaliacao.ItemEntrega;
import br.com.youaudit.extracao.ResultadoValidacao;
import br.com.youaudit.extracao.ValidadorTrechoLiteral;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Confere a resposta do cruzamento ANTES de qualquer uso. Nada aqui confia na IA.
 *
 * <p>Associação: {@code promessa} é um número da lista enviada, respondida uma vez só; se {@code item} não é nulo,
 * é um número da lista de itens E o {@code trecho_entrega} existe literalmente no título ou na descrição DAQUELE
 * item ({@link ValidadorTrechoLiteral}, regra 2). Promessa que a IA não respondeu também é problema.
 *
 * <p>Custo: {@code item} existe e o trecho é literal daquele item. O mesmo item repetido conta uma vez.
 */
@Component
public class AnalisadorRespostaCruzamento {

    private final ValidadorTrechoLiteral validador;
    private final ObjectMapper json = new ObjectMapper();

    public AnalisadorRespostaCruzamento(ValidadorTrechoLiteral validador) {
        this.validador = validador;
    }

    public AnaliseCruzamento analisar(String respostaJson, int totalPromessas, List<ItemEntrega> itens) {
        JsonNode raiz;
        try {
            raiz = json.readTree(respostaJson == null ? "" : respostaJson);
        } catch (JsonProcessingException e) {
            return ilegivel("a resposta não é um JSON válido");
        }
        if (raiz == null || !raiz.path("associacoes").isArray() || !raiz.path("custos").isArray()) {
            return ilegivel("a resposta não segue o esquema: faltam as listas \"associacoes\" e \"custos\"");
        }
        List<AnaliseCruzamento.Problema> problemas = new ArrayList<>();
        List<AnaliseCruzamento.Associacao> associacoes = associacoes(raiz.get("associacoes"), totalPromessas, itens, problemas);
        List<AnaliseCruzamento.Custo> custos = custos(raiz.get("custos"), itens, problemas);
        return new AnaliseCruzamento(associacoes, custos, problemas);
    }

    private List<AnaliseCruzamento.Associacao> associacoes(JsonNode lista, int total, List<ItemEntrega> itens,
                                                           List<AnaliseCruzamento.Problema> problemas) {
        List<AnaliseCruzamento.Associacao> aceitas = new ArrayList<>();
        Set<Integer> respondidas = new HashSet<>();
        for (JsonNode a : lista) {
            Integer promessa = inteiro(a.get("promessa"));
            Integer item = inteiro(a.get("item"));
            String trecho = texto(a.get("trecho_entrega"));
            String descricao = "promessa " + promessa + " → item " + item;
            if (promessa == null || promessa < 0 || promessa >= total) {
                problemas.add(new AnaliseCruzamento.Problema("ASSOCIACAO", descricao, "número de promessa inexistente"));
                continue;
            }
            if (respondidas.contains(promessa)) {
                continue; // repetida: vale a primeira resposta
            }
            if (item != null) {
                String motivo = motivoTrechoInvalido(item, trecho, itens);
                if (motivo != null) {
                    problemas.add(new AnaliseCruzamento.Problema("ASSOCIACAO", descricao + " \"" + trecho + "\"", motivo));
                    continue;
                }
            }
            respondidas.add(promessa);
            aceitas.add(new AnaliseCruzamento.Associacao(promessa, item, item == null ? null : trecho));
        }
        // Promessa sem nenhuma resposta (nem recusada) também é problema: a IA tem que responder sobre todas.
        Set<Integer> recusadas = new HashSet<>();
        for (JsonNode a : lista) {
            Integer promessa = inteiro(a.get("promessa"));
            if (promessa != null && !respondidas.contains(promessa)) {
                recusadas.add(promessa);
            }
        }
        for (int p = 0; p < total; p++) {
            if (!respondidas.contains(p) && !recusadas.contains(p)) {
                problemas.add(new AnaliseCruzamento.Problema("ASSOCIACAO", "promessa " + p, "promessa sem resposta"));
            }
        }
        return aceitas;
    }

    private List<AnaliseCruzamento.Custo> custos(JsonNode lista, List<ItemEntrega> itens,
                                                 List<AnaliseCruzamento.Problema> problemas) {
        List<AnaliseCruzamento.Custo> aceitos = new ArrayList<>();
        Set<Integer> vistos = new HashSet<>();
        for (JsonNode c : lista) {
            Integer item = inteiro(c.get("item"));
            String trecho = texto(c.get("trecho_entrega"));
            String motivo = item == null ? "número de item inexistente" : motivoTrechoInvalido(item, trecho, itens);
            if (motivo != null) {
                problemas.add(new AnaliseCruzamento.Problema("CUSTO", "item " + item + " \"" + trecho + "\"", motivo));
                continue;
            }
            if (vistos.add(item)) {
                aceitos.add(new AnaliseCruzamento.Custo(item, trecho));
            }
        }
        return aceitos;
    }

    /** null se o item existe e o trecho é literal do título ou da descrição dele; senão, o motivo. */
    private String motivoTrechoInvalido(int item, String trecho, List<ItemEntrega> itens) {
        if (item < 0 || item >= itens.size()) {
            return "número de item inexistente";
        }
        ItemEntrega i = itens.get(item);
        ResultadoValidacao noTitulo = validador.validar(trecho, i.titulo());
        if (noTitulo.valido()) {
            return null;
        }
        ResultadoValidacao naDescricao = validador.validar(trecho, i.descricao());
        if (naDescricao.valido()) {
            return null;
        }
        return trecho == null || trecho.isBlank() ? "trecho vazio" : "trecho não encontrado literalmente no item";
    }

    private static Integer inteiro(JsonNode no) {
        return no != null && no.isIntegralNumber() && no.canConvertToInt() ? no.intValue() : null;
    }

    private static String texto(JsonNode no) {
        return no == null || no.isNull() ? null : no.asText();
    }

    private static AnaliseCruzamento ilegivel(String motivo) {
        return new AnaliseCruzamento(List.of(), List.of(), List.of(new AnaliseCruzamento.Problema("RESPOSTA", null, motivo)));
    }
}
