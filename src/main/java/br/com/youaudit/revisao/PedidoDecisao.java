package br.com.youaudit.revisao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpo de POST /api/produtos/{id}/decisao. O motivo é OBRIGATÓRIO: toda decisão humana fica justificada. */
public record PedidoDecisao(@NotNull DecisaoProduto decisao, @NotBlank @Size(max = 2000) String motivo) {
}
