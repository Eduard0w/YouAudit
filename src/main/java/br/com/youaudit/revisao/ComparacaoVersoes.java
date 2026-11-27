package br.com.youaudit.revisao;

import br.com.youaudit.coleta.Origem;
import br.com.youaudit.pontuacao.Faixa;

import java.util.List;

/**
 * GET /api/produtos/{id}/comparacao: o que mudou nos alertas entre a versão anterior e a atual (reenvio).
 *
 * @param decisaoEntreVersoes a última decisão registrada entre a criação da versão anterior e a da atual; nula se a
 *                            pessoa não decidiu nada nesse intervalo
 * @param contagem            números calculados pelo código a partir dos itens (nada estimado)
 */
public record ComparacaoVersoes(String produtoId, Origem origem,
                                int versaoAnterior, Integer pontuacaoAnterior, Faixa faixaAnterior,
                                int versaoAtual, Integer pontuacaoAtual, Faixa faixaAtual,
                                DetalheProduto.DecisaoVista decisaoEntreVersoes, boolean houvePedidoAjuste,
                                List<ComparadorVersoes.Item> itens, Contagem contagem) {

    /**
     * @param alertasAnterior    total de alertas da versão anterior (todos chegaram sem julgamento)
     * @param alertasAtual       total de alertas da versão atual
     * @param semJulgamentoAnterior alertas da versão atual que nenhuma pessoa julgou antes: os NOVOS e os que voltaram
     *                           idênticos de um alerta que ficou PENDENTE na versão anterior
     */
    public record Contagem(int resolvidos, int continuam, int iguaisAnterior, int novos,
                           int alertasAnterior, int alertasAtual, int semJulgamentoAnterior) {
    }
}
