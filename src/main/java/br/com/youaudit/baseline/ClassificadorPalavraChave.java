package br.com.youaudit.baseline;

import br.com.youaudit.avaliacao.AfirmacaoDetectada;
import br.com.youaudit.extracao.Bloco;
import br.com.youaudit.extracao.TipoAfirmacao;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * O "extrator" do baseline: uma frase que contém palavra-chave de um tipo vira uma afirmação daquele tipo.
 * Não entende sentido: "acesso vitalício" e "acesso por 12 meses" são só ACESSO para ele.
 *
 * <p>Por que a FRASE e não o bloco inteiro: o gabarito conta acerto quando os trechos se sobrepõem (um contém o outro).
 * Devolver a linha inteira faria o baseline "acertar" só por mandar um trecho grande; devolver só a palavra-chave
 * ("aulas") o faria errar quase sempre. A frase é o meio-termo justo. Ela é sempre um pedaço literal do bloco.
 */
class ClassificadorPalavraChave {

    private final Map<TipoAfirmacao, List<String>> palavrasPorTipo;
    private final BuscadorTermos buscador;

    ClassificadorPalavraChave(Map<TipoAfirmacao, List<String>> palavrasPorTipo, BuscadorTermos buscador) {
        this.palavrasPorTipo = palavrasPorTipo;
        this.buscador = buscador;
    }

    List<AfirmacaoDetectada> classificar(List<Bloco> blocos) {
        // LinkedHashSet: mantém a ordem e descarta a mesma (bloco, tipo, frase) achada por duas palavras-chave.
        Set<AfirmacaoDetectada> afirmacoes = new LinkedHashSet<>();
        for (Bloco bloco : blocos) {
            for (Map.Entry<TipoAfirmacao, List<String>> tipo : palavrasPorTipo.entrySet()) {
                for (Ocorrencia ocorrencia : buscador.buscar(bloco.texto(), tipo.getValue())) {
                    String frase = frase(bloco.texto(), ocorrencia.inicio(), ocorrencia.fim());
                    afirmacoes.add(new AfirmacaoDetectada(bloco.id(), tipo.getKey(), frase));
                }
            }
        }
        return new ArrayList<>(afirmacoes);
    }

    /**
     * A frase em volta de [inicio, fim): vai até o fim de frase anterior e até o próximo. Fim de frase = ".", "!" ou
     * "?" seguido de espaço ou do fim do texto. Assim o ponto de "R$ 1.200" ou de "v2.0" não corta a frase.
     */
    static String frase(String texto, int inicio, int fim) {
        int comeco = 0;
        for (int i = inicio - 1; i >= 0; i--) {
            if (fimDeFrase(texto, i)) {
                comeco = i + 1;
                break;
            }
        }
        int termino = texto.length();
        for (int i = Math.max(fim - 1, inicio); i < texto.length(); i++) {
            if (fimDeFrase(texto, i)) {
                termino = i + 1;
                break;
            }
        }
        while (comeco < termino && Character.isWhitespace(texto.charAt(comeco))) {
            comeco++;
        }
        while (termino > comeco && Character.isWhitespace(texto.charAt(termino - 1))) {
            termino--;
        }
        return texto.substring(comeco, termino);
    }

    private static boolean fimDeFrase(String texto, int i) {
        char c = texto.charAt(i);
        boolean pontuacao = c == '.' || c == '!' || c == '?';
        return pontuacao && (i + 1 == texto.length() || Character.isWhitespace(texto.charAt(i + 1)));
    }
}
