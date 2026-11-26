package br.com.youaudit.baseline;

/** Um número com unidade achado na página, ex.: "mais de 40 aulas". {@code trecho} é literal do texto original. */
public record QuantidadeEncontrada(int valor, UnidadeQuantidade unidade, Comparacao comparacao, String trecho) {
}
