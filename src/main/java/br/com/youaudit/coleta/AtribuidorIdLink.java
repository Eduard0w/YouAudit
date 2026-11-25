package br.com.youaudit.coleta;

import org.springframework.stereotype.Component;

/**
 * Dá um id anônimo ("link-01", "link-02"...) a cada link novo colado no formulário.
 * O mesmo link recebe sempre o mesmo id, achado pelo SHA-256 da URL ({@code produto.url_hash}): assim a
 * idempotência funciona e a URL em si nunca vai para o banco (regra 8).
 *
 * <p>Limite: dois links novos colados no mesmo instante podem receber o mesmo número. Na demo, com uma pessoa
 * no formulário, isso não acontece; {@code synchronized} reduz a chance, mas não elimina (a transação da primeira
 * pode não ter terminado).
 */
@Component
public class AtribuidorIdLink {

    static final String PREFIXO = "link-";

    private final ProdutoRepository produtos;

    public AtribuidorIdLink(ProdutoRepository produtos) {
        this.produtos = produtos;
    }

    public synchronized String idPara(String urlHash) {
        return produtos.findFirstByUrlHash(urlHash)
                .map(Produto::getId)
                .orElseGet(() -> String.format("%s%02d", PREFIXO, produtos.countByIdStartingWith(PREFIXO) + 1));
    }
}
