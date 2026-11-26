package br.com.youaudit.baseline;

import br.com.youaudit.avaliacao.AchadoDetectado;
import br.com.youaudit.avaliacao.EntradaVerificacao;
import br.com.youaudit.avaliacao.ItemEntrega;
import br.com.youaudit.avaliacao.ResultadoVerificacao;
import br.com.youaudit.coleta.Origem;
import br.com.youaudit.coleta.TipoConteudo;
import br.com.youaudit.config.BaselineConfig;
import br.com.youaudit.config.ExtracaoPropriedades;
import br.com.youaudit.extracao.PreparadorBlocos;
import br.com.youaudit.regras.TipoAchado;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O baseline inteiro, com as peças reais e o baseline-termos.json do projeto, no exemplo fictício
 * "Método Loja Lucrativa" (CONTEXTO.md §3).
 */
class VerificadorBaselineIntegracaoTest {

    private static final String PAGINA = String.join("\n",
            "Método Loja Lucrativa",
            "Fature R$10 mil por mês em 30 dias com a sua loja.",
            "São 40 aulas direto ao ponto.",
            "Bônus: mentoria em grupo.",
            "Acesso 12 meses.",
            "Garantia de 7 dias.",
            "Apenas R$297.");

    @Test
    void exemploDeReferencia() {
        VerificadorBaseline baseline = new VerificadorBaseline(new BaselineConfig().termosBaseline(new ObjectMapper()),
                new PreparadorBlocos(new ExtracaoPropriedades(1200, 8000)));
        List<ItemEntrega> entrega = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            entrega.add(new ItemEntrega(TipoConteudo.MODULO, "Módulo " + i, null));
        }
        for (int i = 1; i <= 28; i++) {
            entrega.add(new ItemEntrega(TipoConteudo.AULA, "Aula " + i,
                    i == 12 ? "Nesta aula usamos uma ferramenta paga de automação." : null));
        }

        ResultadoVerificacao r = baseline.verificar(
                new EntradaVerificacao("simulado-01", Origem.SIMULADO, PAGINA, new BigDecimal("297"), entrega));

        // Os 4 achados do exemplo. O baseline pode achar outros (ex.: INFO_ESSENCIAL_AUSENTE, porque "Acesso 12 meses"
        // e "Apenas R$297" não batem com nenhuma palavra da lista): é a limitação dele, não um erro do teste.
        assertThat(r.achados()).extracting(AchadoDetectado::tipo).contains(
                TipoAchado.PROMESSA_RESULTADO_SEM_RESSALVA,
                TipoAchado.QUANTIDADE_DIVERGENTE,
                TipoAchado.ELEMENTO_AUSENTE_NA_ENTREGA,
                TipoAchado.CUSTO_OMITIDO);
        r.afirmacoes().forEach(a -> assertThat(PAGINA).contains(a.trecho()));
    }
}
