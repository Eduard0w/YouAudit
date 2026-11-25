package br.com.youaudit.coleta;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Transforma a página (HTML ou texto puro) no "texto limpo" do CONTRATOS.md §2.
 * O texto limpo é usado em três lugares e precisa ser o MESMO nos três: no hash, nos blocos enviados à IA
 * e na conferência do trecho literal.
 *
 * <p>Regras, nesta ordem:
 * <ol>
 *   <li>remove as tags HTML; o conteúdo de script e style é descartado;</li>
 *   <li>elementos de bloco (p, div, li, h1... e br) viram quebra de linha, para os parágrafos não se colarem;</li>
 *   <li>espaços, tabs e espaços não separáveis repetidos viram um único espaço;</li>
 *   <li>cada linha é aparada e as linhas vazias são removidas: sobra uma linha por parágrafo;</li>
 *   <li>a caixa das letras NÃO muda: o trecho literal precisa bater caractere por caractere.</li>
 * </ol>
 */
@Component
public class LimpadorTexto {

    /** Tags que, no navegador, começam uma nova linha. */
    private static final Set<String> TAGS_DE_BLOCO = Set.of(
            "p", "div", "br", "li", "ul", "ol", "h1", "h2", "h3", "h4", "h5", "h6",
            "tr", "td", "th", "table", "section", "article", "header", "footer", "main", "nav", "aside",
            "blockquote", "pre", "hr", "form", "dd", "dt", "dl", "figure", "figcaption");

    /** Espaço comum, tab e espaço não separável ( , o &nbsp; do HTML), um ou mais seguidos. */
    private static final Pattern ESPACOS = Pattern.compile("[ \\t\\u00A0]+");

    public String limpar(String pagina) {
        if (pagina == null) {
            return "";
        }
        Document documento = Jsoup.parse(pagina);
        StringBuilder bruto = new StringBuilder();

        // Percorre a árvore do HTML. "head" é chamado ao entrar em cada nó.
        NodeTraversor.traverse(new NodeVisitor() {
            @Override
            public void head(Node no, int profundidade) {
                if (no instanceof TextNode texto) {
                    // getWholeText mantém as quebras de linha originais (importante para .txt).
                    bruto.append(texto.getWholeText());
                } else if (no instanceof Element elemento && TAGS_DE_BLOCO.contains(elemento.normalName())) {
                    bruto.append('\n');
                }
            }

            @Override
            public void tail(Node no, int profundidade) {
                if (no instanceof Element elemento && TAGS_DE_BLOCO.contains(elemento.normalName())) {
                    bruto.append('\n');
                }
            }
        }, documento.body());

        return bruto.toString()
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .lines()
                .map(linha -> ESPACOS.matcher(linha).replaceAll(" ").strip())
                .filter(linha -> !linha.isEmpty())
                .collect(Collectors.joining("\n"));
    }
}
