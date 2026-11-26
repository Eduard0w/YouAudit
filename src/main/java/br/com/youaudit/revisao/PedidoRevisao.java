package br.com.youaudit.revisao;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpo de POST /api/achados/{id}/revisao. A nota é opcional; a decisão, não. */
public record PedidoRevisao(@NotNull DecisaoRevisao decisao, @Size(max = 2000) String nota) {
}
