package br.com.youaudit.baseline;

import br.com.youaudit.extracao.TipoAfirmacao;

import java.util.List;
import java.util.Map;

/**
 * As listas do baseline, lidas de {@code baseline-termos.json} (nunca escritas no código: regra do CLAUDE.md).
 * Os termos podem ter caixa e acento: o {@link BuscadorTermos} ignora os dois. As palavras de {@code unidades} vão
 * em minúsculas e sem acento, porque o {@link ExtratorNumeros} usa regex sobre o texto já normalizado.
 *
 * @param janelaRessalvaBlocos quantos blocos antes e depois da promessa contam como "junto" para achar a ressalva
 * @param informacoesEssenciais nome da informação ("canal de contato") e as palavras que mostram que ela está na página
 */
public record TermosBaseline(
        String fonte,
        List<String> termosProibidos,
        Map<TipoAfirmacao, List<String>> palavrasChavePorTipo,
        List<String> palavrasRessalva,
        int janelaRessalvaBlocos,
        List<String> palavrasCustoNaEntrega,
        Map<String, List<String>> informacoesEssenciais,
        Map<UnidadeQuantidade, List<String>> unidades) {
}
