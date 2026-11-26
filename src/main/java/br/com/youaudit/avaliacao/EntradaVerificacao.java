package br.com.youaudit.avaliacao;

import br.com.youaudit.coleta.Origem;

import java.math.BigDecimal;
import java.util.List;

/**
 * O que um {@link Verificador} recebe: o texto limpo da página e o cadastro do produto.
 * {@code preco} pode ser nulo (página real não tem preço cadastrado). {@code itens} vazio = sem entrega cadastrada.
 */
public record EntradaVerificacao(String produtoId, Origem origem, String textoPagina, BigDecimal preco,
                                 List<ItemEntrega> itens) {

    public EntradaVerificacao {
        textoPagina = textoPagina == null ? "" : textoPagina;
        itens = itens == null ? List.of() : List.copyOf(itens);
    }
}
