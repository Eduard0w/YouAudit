package br.com.youaudit.redacao;

import br.com.youaudit.config.IaConfig;
import br.com.youaudit.extracao.CacheRespostasIa;
import br.com.youaudit.extracao.ClienteIa;
import br.com.youaudit.extracao.FalhaIaException;
import br.com.youaudit.extracao.PedidoIa;
import br.com.youaudit.regras.TipoAchado;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Passo 9 do pipeline: a sugestão de correção de cada achado.
 *
 * <p>Fluxo: uma chamada à IA (modelo secundário) com todos os pontos → o código confere cada sugestão (não vazia, até
 * 600 caracteres, nenhum número que não exista nos dados do produto) → se alguma for recusada, UMA nova tentativa
 * → o que ainda estiver ruim, ou se a IA falhar/estiver sem cota, usa o texto de RESERVA (redacao-modelos.json).
 *
 * <p>Por que a redação nunca derruba a auditoria: a sugestão é um texto de ajuda, não uma evidência. Se a IA cair,
 * a auditoria continua com achados e pontuação; só a sugestão sai do modelo fixo (e a tela mostra a fonte).
 */
@Service
public class ServicoRedacao {

    private static final Logger log = LoggerFactory.getLogger(ServicoRedacao.class);

    private final ClienteIa clienteIa;
    private final CacheRespostasIa cache;
    private final ConferenteNumeros conferente;
    private final ModelosRedacao modelos;
    private final ObjectMapper json = new ObjectMapper();

    public ServicoRedacao(@Qualifier(IaConfig.SECUNDARIO) ClienteIa clienteIa, CacheRespostasIa cache,
                          ConferenteNumeros conferente, ModelosRedacao modelos) {
        this.clienteIa = clienteIa;
        this.cache = cache;
        this.conferente = conferente;
        this.modelos = modelos;
    }

    /** Um achado a redigir: o tipo (para o modelo de reserva), o nome da regra e a evidência. */
    public record Pedido(TipoAchado tipo, String nomeRegra, String evidencia) {
    }

    /**
     * @param dadosDoProduto texto da página + entrega + preço: os números permitidos na sugestão
     * @return uma sugestão por pedido, na mesma ordem
     */
    public List<Sugestao> redigir(List<Pedido> pedidos, String dadosDoProduto) {
        if (pedidos.isEmpty()) {
            return List.of();
        }
        List<PromptRedacao.Ponto> pontos = pedidos.stream().map(p -> new PromptRedacao.Ponto(p.nomeRegra(), p.evidencia())).toList();
        String conteudo = PromptRedacao.conteudo(pontos);
        PedidoIa pedidoIa = new PedidoIa(PromptRedacao.INSTRUCAO_SISTEMA, conteudo, PromptRedacao.esquema());
        String chave = cache.chave(PromptRedacao.TAREFA, PromptRedacao.VERSAO, clienteIa.modelo(), pedidoIa);

        Map<Integer, String> aprovadas = new HashMap<>();
        try {
            Optional<String> emCache = cache.buscar(chave);
            List<String> problemas;
            if (emCache.isPresent()) {
                problemas = conferir(emCache.get(), pedidos.size(), dadosDoProduto, aprovadas);
            } else {
                String primeira = clienteIa.gerarJson(pedidoIa);
                problemas = conferir(primeira, pedidos.size(), dadosDoProduto, aprovadas);
                if (problemas.isEmpty()) {
                    cache.guardar(chave, primeira);
                } else {
                    String segunda = clienteIa.gerarJson(new PedidoIa(pedidoIa.instrucaoSistema(),
                            PromptRedacao.conteudoComCorrecao(conteudo, problemas), pedidoIa.esquemaResposta()));
                    Map<Integer, String> daSegunda = new HashMap<>();
                    if (conferir(segunda, pedidos.size(), dadosDoProduto, daSegunda).isEmpty()) {
                        cache.guardar(chave, segunda);
                    }
                    daSegunda.forEach(aprovadas::putIfAbsent);
                }
            }
        } catch (FalhaIaException e) {
            log.warn("Redação: IA indisponível ({}); usando os textos de reserva", e.getMessage());
        }

        List<Sugestao> sugestoes = new ArrayList<>();
        for (int i = 0; i < pedidos.size(); i++) {
            String daIa = aprovadas.get(i);
            sugestoes.add(daIa != null ? new Sugestao(daIa, FonteSugestao.IA)
                    : new Sugestao(modelos.texto(pedidos.get(i).tipo(), pedidos.get(i).evidencia()), FonteSugestao.MODELO));
        }
        return sugestoes;
    }

    /** Põe em {@code aprovadas} as sugestões que passaram e devolve a lista dos problemas (para a nova tentativa). */
    private List<String> conferir(String respostaJson, int total, String dados, Map<Integer, String> aprovadas) {
        List<String> problemas = new ArrayList<>();
        JsonNode lista;
        try {
            lista = json.readTree(respostaJson == null ? "" : respostaJson).path("sugestoes");
        } catch (Exception e) {
            return List.of("a resposta não é um JSON válido");
        }
        if (!lista.isArray()) {
            return List.of("a resposta não segue o esquema: falta a lista \"sugestoes\"");
        }
        for (JsonNode s : lista) {
            JsonNode ponto = s.get("ponto");
            String texto = s.path("texto").asText("").strip();
            if (ponto == null || !ponto.isIntegralNumber() || ponto.intValue() < 0 || ponto.intValue() >= total) {
                problemas.add("número de ponto inexistente");
                continue;
            }
            int i = ponto.intValue();
            if (texto.isEmpty() || texto.length() > PromptRedacao.TAMANHO_MAXIMO) {
                problemas.add("ponto " + i + ": texto vazio ou com mais de " + PromptRedacao.TAMANHO_MAXIMO + " caracteres");
                continue;
            }
            List<String> inventados = conferente.numerosInventados(texto, dados);
            if (!inventados.isEmpty()) {
                problemas.add("ponto " + i + ": números que não existem nos dados do produto: " + inventados);
                continue;
            }
            aprovadas.putIfAbsent(i, texto);
        }
        for (int i = 0; i < total; i++) {
            String prefixo = "ponto " + i + ":";
            if (!aprovadas.containsKey(i) && problemas.stream().noneMatch(p -> p.startsWith(prefixo))) {
                problemas.add(prefixo + " sem sugestão");
            }
        }
        return problemas;
    }
}
