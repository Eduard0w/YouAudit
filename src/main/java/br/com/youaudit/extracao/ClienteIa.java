package br.com.youaudit.extracao;

/**
 * Acesso à IA, atrás de uma interface (regra do CLAUDE.md): a implementação real é {@link ClienteIaGemini};
 * nos testes entra uma falsa, que devolve respostas programadas sem rede.
 *
 * <p>Devolve o JSON CRU, como veio. Quem chama é obrigado a validar o esquema e conferir os trechos literais antes
 * de usar qualquer coisa: a IA nunca é confiável por padrão.
 */
public interface ClienteIa {

    /** @throws FalhaIaException se não houver resposta utilizável (sem chave, rede, limite, erro da API) */
    String gerarJson(PedidoIa pedido);

    /** Nome do modelo que responde. Entra na chave do cache: outro modelo, outra resposta. */
    String modelo();
}
