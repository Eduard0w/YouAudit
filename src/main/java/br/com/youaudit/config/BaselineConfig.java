package br.com.youaudit.config;

import br.com.youaudit.baseline.TermosBaseline;
import br.com.youaudit.baseline.UnidadeQuantidade;
import br.com.youaudit.extracao.TipoAfirmacao;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

/**
 * Lê {@code baseline-termos.json} uma vez, no início. Se o arquivo estiver incompleto, a aplicação NÃO sobe:
 * melhor falhar cedo do que rodar um baseline com uma lista vazia e comparar de forma injusta.
 */
@Configuration
public class BaselineConfig {

    static final String ARQUIVO = "baseline-termos.json";

    @Bean
    public TermosBaseline termosBaseline(ObjectMapper mapper) {
        try (InputStream entrada = new ClassPathResource(ARQUIVO).getInputStream()) {
            return validar(mapper.readValue(entrada, TermosBaseline.class));
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui ler " + ARQUIVO, e);
        }
    }

    /** Público para o teste conferir cada caso de arquivo inválido sem subir o Spring. */
    public static TermosBaseline validar(TermosBaseline t) {
        exigirLista("termosProibidos", t.termosProibidos());
        exigirLista("palavrasRessalva", t.palavrasRessalva());
        exigirLista("palavrasCustoNaEntrega", t.palavrasCustoNaEntrega());
        if (t.janelaRessalvaBlocos() < 0) {
            throw new IllegalStateException(ARQUIVO + ": janelaRessalvaBlocos não pode ser negativa");
        }
        exigirMapa("palavrasChavePorTipo", t.palavrasChavePorTipo());
        for (TipoAfirmacao tipo : TipoAfirmacao.values()) {
            // OUTRO é o "não sei dizer" da IA: não existe palavra-chave para ele.
            if (tipo != TipoAfirmacao.OUTRO) {
                exigirLista("palavrasChavePorTipo." + tipo, t.palavrasChavePorTipo().get(tipo));
            }
        }
        exigirMapa("informacoesEssenciais", t.informacoesEssenciais());
        t.informacoesEssenciais().forEach((nome, palavras) -> exigirLista("informacoesEssenciais." + nome, palavras));
        exigirMapa("unidades", t.unidades());
        for (UnidadeQuantidade unidade : UnidadeQuantidade.values()) {
            List<String> palavras = t.unidades().get(unidade);
            exigirLista("unidades." + unidade, palavras);
            for (String palavra : palavras) {
                if (!palavra.matches("[a-z]+")) {
                    throw new IllegalStateException(ARQUIVO + ": unidades." + unidade + " aceita só letras minúsculas"
                            + " sem acento (o ExtratorNumeros compara com o texto normalizado): \"" + palavra + "\"");
                }
            }
        }
        return t;
    }

    private static void exigirLista(String campo, List<String> lista) {
        if (lista == null || lista.isEmpty() || lista.stream().anyMatch(s -> s == null || s.isBlank())) {
            throw new IllegalStateException(ARQUIVO + ": " + campo + " precisa de pelo menos um termo e nenhum vazio");
        }
    }

    private static void exigirMapa(String campo, Map<?, ?> mapa) {
        if (mapa == null || mapa.isEmpty()) {
            throw new IllegalStateException(ARQUIVO + ": " + campo + " está ausente ou vazio");
        }
    }
}
