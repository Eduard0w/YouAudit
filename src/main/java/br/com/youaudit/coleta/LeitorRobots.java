package br.com.youaudit.coleta;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Decide se o robots.txt de um site permite acessar um caminho. Versão simples do padrão (RFC 9309):
 * <ul>
 *   <li>usa o grupo cujo "User-agent" aparece no nosso nome; se não houver, o grupo "*";</li>
 *   <li>entre as regras Allow/Disallow que casam com o caminho, vale a de caminho mais longo;
 *       em empate, Allow vence;</li>
 *   <li>entende "*" (qualquer sequência) e "$" (fim do caminho);</li>
 *   <li>"Disallow:" vazio não proíbe nada.</li>
 * </ul>
 * Escrito à mão para não adicionar dependência (regra do CLAUDE.md).
 */
@Component
public class LeitorRobots {

    private record Regra(String caminho, boolean permite) {
    }

    private static final class Grupo {
        private final List<String> agentes = new ArrayList<>();
        private final List<Regra> regras = new ArrayList<>();
    }

    /**
     * @param robotsTxt conteúdo do robots.txt ("" = sem regras)
     * @param agente    nome curto do nosso robô, ex.: "youaudit-prototipo"
     * @param caminho   caminho da URL com a query, ex.: "/curso?ref=1"
     */
    public boolean permite(String robotsTxt, String agente, String caminho) {
        Grupo grupo = escolherGrupo(ler(robotsTxt), agente.toLowerCase(Locale.ROOT));
        if (grupo == null) {
            return true;
        }
        int maiorTamanho = -1;
        boolean permitido = true;
        for (Regra regra : grupo.regras) {
            if (regra.caminho().isEmpty() || !casa(regra.caminho(), caminho)) {
                continue;
            }
            int tamanho = regra.caminho().length();
            if (tamanho > maiorTamanho || (tamanho == maiorTamanho && regra.permite())) {
                maiorTamanho = tamanho;
                permitido = regra.permite();
            }
        }
        return permitido;
    }

    /** Separa o arquivo em grupos: linhas User-agent seguidas formam um grupo, e as regras abaixo são dele. */
    private static List<Grupo> ler(String robotsTxt) {
        List<Grupo> grupos = new ArrayList<>();
        Grupo atual = null;
        boolean ultimaLinhaFoiAgente = false;
        for (String linhaBruta : robotsTxt.lines().toList()) {
            String linha = linhaBruta.replaceFirst("#.*", "").strip();
            int doisPontos = linha.indexOf(':');
            if (doisPontos < 0) {
                continue;
            }
            String chave = linha.substring(0, doisPontos).strip().toLowerCase(Locale.ROOT);
            String valor = linha.substring(doisPontos + 1).strip();
            switch (chave) {
                case "user-agent" -> {
                    if (atual == null || !ultimaLinhaFoiAgente) {
                        atual = new Grupo();
                        grupos.add(atual);
                    }
                    atual.agentes.add(valor.toLowerCase(Locale.ROOT));
                    ultimaLinhaFoiAgente = true;
                }
                case "allow", "disallow" -> {
                    if (atual != null) {
                        atual.regras.add(new Regra(valor, chave.equals("allow")));
                    }
                    ultimaLinhaFoiAgente = false;
                }
                default -> {
                    // Outras diretivas (Crawl-delay, Sitemap...) são ignoradas.
                }
            }
        }
        return grupos;
    }

    private static Grupo escolherGrupo(List<Grupo> grupos, String agente) {
        Grupo curinga = null;
        for (Grupo grupo : grupos) {
            for (String nome : grupo.agentes) {
                if (nome.equals("*")) {
                    if (curinga == null) {
                        curinga = grupo;
                    }
                } else if (agente.contains(nome)) {
                    return grupo;
                }
            }
        }
        return curinga;
    }

    /** Converte o caminho da regra em expressão regular: "*" vira ".*", "$" no fim ancora o final. */
    private static boolean casa(String padrao, String caminho) {
        boolean ancorado = padrao.endsWith("$");
        String semAncora = ancorado ? padrao.substring(0, padrao.length() - 1) : padrao;
        StringBuilder regex = new StringBuilder();
        for (String parte : semAncora.split("\\*", -1)) {
            if (!regex.isEmpty()) {
                regex.append(".*");
            }
            regex.append(Pattern.quote(parte));
        }
        var matcher = Pattern.compile(regex.toString()).matcher(caminho);
        return ancorado ? matcher.matches() : matcher.lookingAt();
    }
}
