package br.com.youaudit.entrega;

import java.util.List;

/**
 * O que o {@link AnalisadorRespostaCruzamento} aceitou e recusou de UMA resposta da IA.
 *
 * @param associacoes  uma por promessa respondida corretamente; {@code item} nulo = "nenhum item corresponde"
 * @param custos       itens com custo/requisito pago, com trecho literal conferido
 * @param problemas    o que foi recusado, com o motivo (vai para a nova tentativa e para descarte_cruzamento)
 */
public record AnaliseCruzamento(List<Associacao> associacoes, List<Custo> custos, List<Problema> problemas) {

    public record Associacao(int promessa, Integer item, String trechoEntrega) {
    }

    public record Custo(int item, String trechoEntrega) {
    }

    /** {@code tipo} = "ASSOCIACAO", "CUSTO" ou "RESPOSTA" (a resposta inteira). */
    public record Problema(String tipo, String conteudo, String motivo) {

        public String paraCorrecao() {
            return conteudo == null ? motivo : conteudo + ": " + motivo;
        }
    }

    public boolean totalmenteValida() {
        return problemas.isEmpty();
    }
}
