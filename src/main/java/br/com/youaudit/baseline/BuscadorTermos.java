package br.com.youaudit.baseline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Procura termos num texto ignorando caixa, acentos e diferenças de espaço, respeitando palavra inteira.
 * Devolve o trecho como está no texto original. Especificação: youaudit-docs/ESPEC-baseline-eduardo.md, peça 1.
 *
 * <p>Como funciona: procura no texto NORMALIZADO ({@link NormalizadorTexto}: minúsculas, sem acento, espaço único,
 * mesmo tamanho do original) e recorta o trecho do ORIGINAL nas mesmas posições. Por isso o trecho é sempre literal.
 *
 * <p>Não é {@code final} porque os testes do VerificadorBaseline usam um dublê.
 */
public class BuscadorTermos {

    /** Antes e depois do termo não pode haver letra nem dígito: "garantido" não casa em "garantidora". */
    private static final String SEM_LETRA_ANTES = "(?<![\\p{L}\\p{N}])";
    private static final String SEM_LETRA_DEPOIS = "(?![\\p{L}\\p{N}])";

    public List<Ocorrencia> buscar(String texto, Collection<String> termos) {
        List<Ocorrencia> resultado = new ArrayList<>();

        if (texto == null || texto.isEmpty() || termos == null) {
            return resultado;
        }

        String normalizado = NormalizadorTexto.normalizar(texto);
        // Termos iguais depois de normalizar ("bônus" e "bonus") são procurados uma vez só.
        Set<String> jaProcurados = new HashSet<>();

        for (String bruto : termos) {
            if (bruto == null || bruto.isBlank()) {
                continue;
            }
            String termo = bruto.trim();
            String termoNormalizado = NormalizadorTexto.normalizar(termo);
            if (!jaProcurados.add(termoNormalizado)) {
                continue;
            }

            Matcher m = padrao(termoNormalizado).matcher(normalizado);
            while (m.find()) {
                int inicio = m.start();
                int fim = m.end();
                resultado.add(new Ocorrencia(termo, inicio, fim, texto.substring(inicio, fim)));
            }
        }

        // Por início; no empate, o trecho maior primeiro ("acesso vitalício" antes de "acesso").
        resultado.sort(Comparator.comparingInt(Ocorrencia::inicio)
                .thenComparing(Comparator.comparingInt(Ocorrencia::fim).reversed()));
        return resultado;
    }

    /**
     * Cada palavra do termo vira texto literal ({@code Pattern.quote}: "100%" e "(grátis)" não viram regex) e os
     * espaços entre elas viram {@code \s+}, para casar "renda   extra". No texto normalizado todo espaço já é " ",
     * inclusive o  , que o {@code \s} do Java não reconheceria sozinho.
     */
    private static Pattern padrao(String termoNormalizado) {
        List<String> palavras = Arrays.stream(termoNormalizado.split(" +")).map(Pattern::quote).toList();
        return Pattern.compile(SEM_LETRA_ANTES + String.join("\\s+", palavras) + SEM_LETRA_DEPOIS);
    }
}
