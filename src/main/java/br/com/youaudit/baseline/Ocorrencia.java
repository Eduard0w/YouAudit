package br.com.youaudit.baseline;

/**
 * Um termo achado num texto. {@code trecho} é SEMPRE {@code texto.substring(inicio, fim)} do texto ORIGINAL
 * (com a caixa, os acentos e os espaços de lá), para valer como evidência literal.
 */
public record Ocorrencia(String termo, int inicio, int fim, String trecho) {
}
