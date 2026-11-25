package br.com.youaudit.extracao;

/** Um pedaço numerado do texto limpo. {@code texto} é um trecho EXATO do texto da página, sem nenhuma alteração. */
public record Bloco(int id, String texto) {
}
