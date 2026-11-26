package br.com.youaudit.baseline;

import java.text.Normalizer;

/**
 * Deixa o texto em minúsculas, sem acento e com todo espaço em branco virando " ", SEM MUDAR O TAMANHO.
 *
 * <p>Por que o tamanho importa: procuramos no texto normalizado, mas o trecho devolvido precisa vir do ORIGINAL
 * (regra do trecho literal). Se "á" virasse dois caracteres (o que o {@code Normalizer} faz com a string inteira),
 * a posição 10 no normalizado deixaria de ser a posição 10 no original. Trocando caractere por caractere, uma posição
 * vale nos dois textos e basta fazer {@code original.substring(inicio, fim)}.
 */
final class NormalizadorTexto {

    private NormalizadorTexto() {
    }

    static String normalizar(String texto) {
        StringBuilder sb = new StringBuilder(texto.length());
        for (int i = 0; i < texto.length(); i++) {
            sb.append(normalizar(texto.charAt(i)));
        }
        return sb.toString();
    }

    private static char normalizar(char c) {
        // isWhitespace não reconhece o espaço inseparável ( , o &nbsp; do HTML); isSpaceChar reconhece.
        if (Character.isWhitespace(c) || Character.isSpaceChar(c)) {
            return ' ';
        }
        // NFD separa a letra do acento: "ç" vira "c" + cedilha. Ficamos só com a letra (o primeiro caractere).
        String decomposto = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD);
        return Character.toLowerCase(decomposto.charAt(0));
    }
}
