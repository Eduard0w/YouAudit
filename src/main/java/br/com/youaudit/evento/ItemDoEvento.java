package br.com.youaudit.evento;

import br.com.youaudit.coleta.TipoConteudo;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Um item da entrega no evento (módulo, aula, bônus ou certificado). */
public record ItemDoEvento(
        @NotNull TipoConteudo tipo,
        @NotBlank String titulo,
        String descricao,
        @PositiveOrZero @JsonProperty("duracao_min") Integer duracaoMin) {
}
