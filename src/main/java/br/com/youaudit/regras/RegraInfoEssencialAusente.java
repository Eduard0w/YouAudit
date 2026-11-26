package br.com.youaudit.regras;

import br.com.youaudit.avaliacao.AchadoDetectado;
import br.com.youaudit.baseline.BuscadorTermos;
import br.com.youaudit.extracao.TipoAfirmacao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * INFO_ESSENCIAL_AUSENTE: falta na página preço/condições, prazo de acesso, canal de contato ou política de reembolso.
 *
 * <p>Uma informação conta como PRESENTE se aparecer de um destes dois jeitos:
 * <ol>
 *   <li>uma palavra da lista {@code informacoesEssenciais} de {@code baseline-termos.json} (como o baseline faz);</li>
 *   <li>ou uma afirmação que a IA classificou com o tipo correspondente (ex.: "devolvemos 100% do seu dinheiro" é
 *       GARANTIA mesmo sem a palavra "reembolso"). Esse é o ganho sobre o baseline.</li>
 * </ol>
 * O tipo de cada informação está em {@link #TIPO_QUE_COMPROVA}. "canal de contato" não tem tipo de afirmação
 * correspondente, então só a lista de palavras vale.
 *
 * <p>Não tem trecho: o problema é o que NÃO está escrito. Limite: o tipo ACESSO inclui "acesso imediato", que não
 * diz o prazo; nesse caso a informação conta como presente (pode esconder uma ausência).
 */
public class RegraInfoEssencialAusente implements RegraDeterministica {

    /** Chaves = nomes usados em informacoesEssenciais do baseline-termos.json. */
    static final Map<String, TipoAfirmacao> TIPO_QUE_COMPROVA = Map.of(
            "preço e condições", TipoAfirmacao.PRECO_CONDICOES,
            "prazo de acesso", TipoAfirmacao.ACESSO,
            "política de reembolso", TipoAfirmacao.GARANTIA);

    private final Map<String, List<String>> informacoes;
    private final BuscadorTermos buscador;

    public RegraInfoEssencialAusente(Map<String, List<String>> informacoes, BuscadorTermos buscador) {
        this.informacoes = Map.copyOf(informacoes);
        this.buscador = buscador;
    }

    @Override
    public List<AchadoDetectado> avaliar(ContextoRegras contexto) {
        List<AchadoDetectado> achados = new ArrayList<>();
        // Ordem estável (alfabética) para o resultado não mudar de uma execução para outra.
        informacoes.keySet().stream().sorted().forEach(nome -> {
            if (!presente(nome, informacoes.get(nome), contexto)) {
                achados.add(new AchadoDetectado(TipoAchado.INFO_ESSENCIAL_AUSENTE, null, null,
                        "Não encontrei na página: " + nome + "."));
            }
        });
        return achados;
    }

    private boolean presente(String nome, List<String> palavras, ContextoRegras contexto) {
        TipoAfirmacao tipo = TIPO_QUE_COMPROVA.get(nome);
        if (tipo != null && contexto.afirmacoes().stream().anyMatch(a -> a.tipo() == tipo)) {
            return true;
        }
        return contexto.blocos().stream().anyMatch(b -> !buscador.buscar(b.texto(), palavras).isEmpty());
    }
}
