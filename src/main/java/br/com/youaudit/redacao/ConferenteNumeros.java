package br.com.youaudit.redacao;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Confere o texto que a IA redigiu (CONTRATOS.md §4 passo 9): todo NÚMERO da sugestão precisa já existir nos dados
 * do produto (página, entrega ou preço). Assim a IA não inventa "garantia de 30 dias" nem "R$ 49" (regra 5).
 *
 * <p>Como: cada número (da sugestão e dos dados) vira uma forma "canônica": sem ",00"/".00" no fim e sem pontos e
 * vírgulas ("R$ 1.297,00" → "1297"; "1.200" → "1200"). Um número da sugestão passa se a forma canônica dele existe
 * entre as dos dados. Limites declarados: não pega número por extenso ("trinta dias"), e "1,5" e "15" ficam iguais.
 */
@Component
public class ConferenteNumeros {

    private static final Pattern NUMERO = Pattern.compile("\\d+(?:[.,]\\d+)*");

    /** Os números da sugestão que NÃO aparecem nos dados. Lista vazia = aprovado. */
    public List<String> numerosInventados(String sugestao, String dadosDoProduto) {
        List<String> inventados = new ArrayList<>();
        if (sugestao == null) {
            return inventados;
        }
        Set<String> permitidos = new HashSet<>();
        Matcher d = NUMERO.matcher(dadosDoProduto == null ? "" : dadosDoProduto);
        while (d.find()) {
            permitidos.add(canonico(d.group()));
        }
        Matcher m = NUMERO.matcher(sugestao);
        while (m.find()) {
            if (!permitidos.contains(canonico(m.group()))) {
                inventados.add(m.group());
            }
        }
        return inventados;
    }

    static String canonico(String numero) {
        String semCentavosZerados = numero.matches(".*[.,]00") ? numero.substring(0, numero.length() - 3) : numero;
        return semCentavosZerados.replaceAll("[.,]", "");
    }
}
