package br.com.youaudit.evento;

import br.com.youaudit.coleta.Origem;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Dados do produto que chegam no evento. {@code conteudo} é opcional: sem ele, o cruzamento com a entrega é pulado.
 * {@code @JsonProperty} liga o nome do JSON (snake_case) ao nome Java (camelCase).
 */
public record ProdutoDoEvento(
        @NotBlank String id,
        @NotBlank String nome,
        String categoria,
        @NotNull @PositiveOrZero BigDecimal preco,
        @NotNull Origem origem,
        @NotBlank @JsonProperty("pagina_texto") String paginaTexto,
        @Valid ConteudoDoEvento conteudo) {
}
