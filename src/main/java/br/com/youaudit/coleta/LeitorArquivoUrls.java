package br.com.youaudit.coleta;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Lê o data/real/urls.txt. Formato: uma página por linha, "pagina-01 https://...".
 * Linhas em branco e linhas que começam com "#" são ignoradas.
 * O id vem escrito na linha (e não da posição) para que reordenar o arquivo não troque "pagina-01" de página:
 * o gabarito referencia esses ids.
 * Inválida: id fora do formato "pagina-NN", URL sem http(s), mais ou menos de duas partes, ou id repetido.
 */
@Component
public class LeitorArquivoUrls {

    private static final Pattern ID = Pattern.compile("pagina-\\d{2,}");
    private static final Pattern URL = Pattern.compile("https?://\\S+");

    public LeituraUrls ler(Path arquivo) throws IOException {
        List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        List<LinhaUrl> validas = new ArrayList<>();
        List<Integer> invalidas = new ArrayList<>();
        Set<String> idsVistos = new HashSet<>();

        for (int i = 0; i < linhas.size(); i++) {
            int numero = i + 1;
            String linha = linhas.get(i).strip();
            if (linha.isEmpty() || linha.startsWith("#")) {
                continue;
            }
            String[] partes = linha.split("\\s+");
            boolean ok = partes.length == 2
                    && ID.matcher(partes[0]).matches()
                    && URL.matcher(partes[1]).matches()
                    && idsVistos.add(partes[0]);
            if (ok) {
                validas.add(new LinhaUrl(numero, partes[0], partes[1]));
            } else {
                invalidas.add(numero);
            }
        }
        return new LeituraUrls(validas, invalidas);
    }
}
