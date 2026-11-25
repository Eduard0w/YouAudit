package br.com.youaudit.evento;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Corpo do POST /api/eventos/produto-enviado (CONTRATOS.md §3). */
public record EventoProdutoRequest(
        @NotNull TipoEvento tipo,
        @NotNull @Valid ProdutoDoEvento produto) {
}
