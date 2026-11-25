package br.com.youaudit.coleta;

import br.com.youaudit.config.ColetaPropriedades;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Grava a cópia (snapshot) de uma página coletada, para a auditoria poder ser conferida depois
 * mesmo que o site mude. Pasta: {diretorio}/{produtoId}/{data UTC}-{12 primeiros caracteres do hash}/ com
 * pagina.html (HTML bruto), texto.txt (texto limpo, o que vale para hash e trecho literal) e meta.json.
 * O diretório fica dentro de data/real/, que NUNCA vai para o Git.
 */
@Component
public class GravadorSnapshot {

    private static final DateTimeFormatter CARIMBO =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneOffset.UTC);

    private final Path diretorio;
    private final ObjectMapper json = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public GravadorSnapshot(ColetaPropriedades propriedades) {
        this.diretorio = propriedades.diretorioSnapshots();
    }

    public Path gravar(String produtoId, String url, String html, String texto, String hash, Instant coletadoEm) {
        // Só letras, números, "-" e "_": um id como "../x" não consegue gravar fora do diretório.
        String nomeSeguro = produtoId.replaceAll("[^A-Za-z0-9_-]", "_");
        Path pasta = diretorio.resolve(nomeSeguro).resolve(CARIMBO.format(coletadoEm) + "-" + hash.substring(0, 12));

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("produto_id", produtoId);
        meta.put("url", url);
        meta.put("coletado_em", coletadoEm.toString());
        meta.put("hash_texto", hash);
        meta.put("caracteres_texto", texto.length());
        try {
            Files.createDirectories(pasta);
            Files.writeString(pasta.resolve("pagina.html"), html, StandardCharsets.UTF_8);
            Files.writeString(pasta.resolve("texto.txt"), texto, StandardCharsets.UTF_8);
            json.writeValue(pasta.resolve("meta.json").toFile(), meta);
        } catch (IOException e) {
            throw new ColetaFalhouException("não foi possível gravar o snapshot no disco");
        }
        return pasta;
    }
}
