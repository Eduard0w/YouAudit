package br.com.youaudit.evento;

import br.com.youaudit.coleta.Origem;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Dados do produto que chegam no evento. A página vem por {@code url} (o sistema baixa) ou por {@code pagina_texto}.
 * Pelo menos um dos dois é obrigatório; se vierem os dois, vale a URL.
 * {@code id} e {@code nome} são obrigatórios com entrada EVENTO (conferido em {@link EventoProdutoRequest}).
 * {@code preco} é obrigatório só sem URL: página real não tem preço cadastrado.
 * {@code conteudo} é opcional: sem ele, o cruzamento com a entrega é pulado.
 * {@code @JsonProperty} liga o nome do JSON (snake_case) ao nome Java (camelCase).
 */
public record ProdutoDoEvento(
        String id,
        String nome,
        String categoria,
        @PositiveOrZero BigDecimal preco,
        @NotNull Origem origem,
        @Pattern(regexp = "^https?://\\S+$", message = "deve começar com http:// ou https://") String url,
        @JsonProperty("pagina_texto") String paginaTexto,
        @Valid ConteudoDoEvento conteudo) {

    /** O Bean Validation chama este método e responde 400 se devolver false. @JsonIgnore: não vira campo do JSON. */
    @JsonIgnore
    @AssertTrue(message = "informe produto.url ou produto.pagina_texto")
    public boolean isUrlOuTextoInformado() {
        return temUrl() || (paginaTexto != null && !paginaTexto.isBlank());
    }

    @JsonIgnore
    @AssertTrue(message = "produto.preco é obrigatório quando não há url")
    public boolean isPrecoInformadoSemUrl() {
        return temUrl() || preco != null;
    }

    @JsonIgnore
    public boolean temUrl() {
        return url != null && !url.isBlank();
    }

    /** Cópia com outro id e nome (usado quando o sistema atribui "link-NN"). */
    public ProdutoDoEvento comIdENome(String novoId, String novoNome) {
        return new ProdutoDoEvento(novoId, novoNome, categoria, preco, origem, url, paginaTexto, conteudo);
    }
}
