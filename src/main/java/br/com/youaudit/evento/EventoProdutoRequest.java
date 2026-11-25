package br.com.youaudit.evento;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo do POST /api/eventos/produto-enviado (CONTRATOS.md §3).
 * {@code entrada} é opcional (padrão EVENTO). Com LINK, o produto só precisa de url e origem:
 * o sistema atribui o id "link-NN" (o nome real da página nunca é usado, regra 8).
 */
public record EventoProdutoRequest(
        @NotNull TipoEvento tipo,
        EntradaAuditoria entrada,
        @NotNull @Valid ProdutoDoEvento produto) {

    /** Normaliza: entrada ausente vira EVENTO. */
    public EventoProdutoRequest {
        entrada = entrada == null ? EntradaAuditoria.EVENTO : entrada;
    }

    /** Atalho para quem não informa a entrada (fica EVENTO). */
    public EventoProdutoRequest(TipoEvento tipo, ProdutoDoEvento produto) {
        this(tipo, null, produto);
    }

    @JsonIgnore
    @AssertTrue(message = "com entrada EVENTO, produto.id e produto.nome são obrigatórios")
    public boolean isIdENomeInformados() {
        if (entrada == EntradaAuditoria.LINK || produto == null) {
            return true;
        }
        return naoVazio(produto.id()) && naoVazio(produto.nome());
    }

    @JsonIgnore
    @AssertTrue(message = "com entrada LINK, produto.url é obrigatória")
    public boolean isLinkComUrl() {
        return entrada != EntradaAuditoria.LINK || produto == null || produto.temUrl();
    }

    private static boolean naoVazio(String texto) {
        return texto != null && !texto.isBlank();
    }
}
