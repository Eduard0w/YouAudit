package br.com.youaudit.baseline;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Acha quantidades com unidade ("40 aulas", "+5 módulos", "mais de 1.200 videoaulas") por regex.
 * Especificação: youaudit-docs/ESPEC-baseline-eduardo.md, peça 2.
 *
 * <p>Roda UM regex sobre o texto normalizado ({@link NormalizadorTexto}: minúsculas, sem acento, mesmo tamanho), então
 * "Módulos" e "MODULOS" são iguais para o regex e o trecho sai do original pelas mesmas posições.
 *
 * <p>Limites declarados: não lê número por extenso ("quarenta aulas") e trata "até 40 aulas" como exatamente 40.
 */
public class ExtratorNumeros {

    /*
     * Partes do padrão, na ordem:
     *  prefixo opcional: "mais de" / "pelo menos" (palavra inteira) ou "+" com espaços opcionais;
     *  número: não pode vir logo depois de letra, dígito, "." ou "," (senão "1,5 aulas" daria "5 aulas");
     *          milhar com ponto ("1.200") ou só dígitos;
     *  pelo menos um espaço e a unidade, como palavra inteira.
     */
    private static final String PREFIXO =
            "(?:(?<![\\p{L}\\p{N}])(?<palavra>mais\\s+de|pelo\\s+menos)\\s+|(?<mais>\\+)\\s*)?";
    private static final String NUMERO = "(?<![\\p{L}\\p{N}.,])(?<numero>\\d{1,3}(?:\\.\\d{3})+|\\d+)";

    private final Pattern padrao;
    private final Map<String, UnidadeQuantidade> unidadePorPalavra = new HashMap<>();

    /** @param unidades palavras de cada unidade, já em minúsculas e sem acento (vêm de baseline-termos.json) */
    public ExtratorNumeros(Map<UnidadeQuantidade, List<String>> unidades) {
        unidades.forEach((unidade, palavras) -> palavras.forEach(p -> unidadePorPalavra.put(p, unidade)));
        // Palavras mais longas primeiro: num "|", o regex tenta da esquerda para a direita.
        List<String> palavras = new ArrayList<>(unidadePorPalavra.keySet());
        palavras.sort(Comparator.comparingInt(String::length).reversed());
        String alternativas = palavras.isEmpty()
                ? "(?!)" // nenhuma unidade configurada: um padrão que nunca casa
                : String.join("|", palavras.stream().map(Pattern::quote).toList());
        this.padrao = Pattern.compile(PREFIXO + NUMERO + "\\s+(?<unidade>" + alternativas + ")(?![\\p{L}\\p{N}])");
    }

    public List<QuantidadeEncontrada> extrair(String texto) {
        List<QuantidadeEncontrada> encontradas = new ArrayList<>();
        if (texto == null || texto.isEmpty()) {
            return encontradas;
        }
        Matcher m = padrao.matcher(NormalizadorTexto.normalizar(texto));
        while (m.find()) {
            Integer valor = valor(m.group("numero"));
            if (valor == null) {
                continue; // número grande demais para int: ignora este trecho
            }
            encontradas.add(new QuantidadeEncontrada(valor, unidadePorPalavra.get(m.group("unidade")),
                    comparacao(m), texto.substring(m.start(), m.end())));
        }
        return encontradas;
    }

    private static Comparacao comparacao(Matcher m) {
        if (m.group("mais") != null) {
            return Comparacao.PELO_MENOS;
        }
        String palavra = m.group("palavra");
        if (palavra == null) {
            return Comparacao.EXATO;
        }
        return palavra.startsWith("mais") ? Comparacao.MAIS_QUE : Comparacao.PELO_MENOS;
    }

    private static Integer valor(String numero) {
        try {
            return Integer.parseInt(numero.replace(".", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
