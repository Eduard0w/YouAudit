package br.com.youaudit.pontuacao;

import br.com.youaudit.avaliacao.AchadoDetectado;
import br.com.youaudit.avaliacao.AfirmacaoDetectada;
import br.com.youaudit.avaliacao.ItemEntrega;
import br.com.youaudit.baseline.TermosBaseline;
import br.com.youaudit.coleta.TipoConteudo;
import br.com.youaudit.config.BaselineConfig;
import br.com.youaudit.config.ExtracaoPropriedades;
import br.com.youaudit.config.PontuacaoPropriedades;
import br.com.youaudit.entrega.AnaliseCruzamento;
import br.com.youaudit.entrega.AnalisadorRespostaCruzamento;
import br.com.youaudit.extracao.PreparadorBlocos;
import br.com.youaudit.extracao.TipoAfirmacao;
import br.com.youaudit.extracao.ValidadorTrechoLiteral;
import br.com.youaudit.regras.ContextoRegras;
import br.com.youaudit.regras.MotorRegras;
import br.com.youaudit.regras.TipoAchado;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O exemplo de referência do CONTEXTO.md §3, de ponta a ponta no código (sem rede): as afirmações e a resposta do
 * cruzamento são o que a IA deveria devolver; regras, validação e pontuação são as de verdade.
 * Esperado: promessa sem ressalva (20) + quantidade divergente (15) + bônus ausente (25) + custo omitido (30) = 90, ALTA.
 */
class ExemploLojaLucrativaTest {

    private static final String PAGINA = """
            Método Loja Lucrativa
            Fature R$10 mil por mês em 30 dias
            São 40 aulas práticas
            Bônus: mentoria em grupo
            Acesso por 12 meses
            Garantia de 7 dias
            Apenas R$297 no pix
            Dúvidas? Fale com o suporte pelo e-mail""";

    @Test
    void dá90EAlta() {
        TermosBaseline termos = new BaselineConfig().termosBaseline(new ObjectMapper());
        PreparadorBlocos preparador = new PreparadorBlocos(new ExtracaoPropriedades(1200, 8000));

        List<AfirmacaoDetectada> afirmacoes = List.of(
                new AfirmacaoDetectada(2, TipoAfirmacao.RESULTADO_FINANCEIRO, "Fature R$10 mil por mês"),
                new AfirmacaoDetectada(2, TipoAfirmacao.PRAZO_RESULTADO, "em 30 dias"),
                new AfirmacaoDetectada(3, TipoAfirmacao.QUANTIDADE_CONTEUDO, "40 aulas práticas"),
                new AfirmacaoDetectada(4, TipoAfirmacao.BONUS, "Bônus: mentoria em grupo"),
                new AfirmacaoDetectada(5, TipoAfirmacao.ACESSO, "Acesso por 12 meses"),
                new AfirmacaoDetectada(6, TipoAfirmacao.GARANTIA, "Garantia de 7 dias"),
                new AfirmacaoDetectada(7, TipoAfirmacao.PRECO_CONDICOES, "Apenas R$297"));
        List<ItemEntrega> itens = entrega();

        List<AchadoDetectado> achados = new ArrayList<>(
                new MotorRegras(termos).avaliar(new ContextoRegras(preparador.dividir(PAGINA), afirmacoes, itens)));

        // Cruzamento: a resposta que a IA deveria dar, passando pela MESMA validação do código.
        AnaliseCruzamento cruzamento = new AnalisadorRespostaCruzamento(new ValidadorTrechoLiteral()).analisar("""
                {"associacoes":[{"promessa":0,"item":null}],
                 "custos":[{"item":%d,"trecho_entrega":"Requer a ferramenta paga X"}]}""".formatted(itens.size() - 1), 1, itens);
        assertThat(cruzamento.totalmenteValida()).isTrue();
        achados.add(new AchadoDetectado(TipoAchado.ELEMENTO_AUSENTE_NA_ENTREGA, "Bônus: mentoria em grupo", null, ""));
        achados.add(new AchadoDetectado(TipoAchado.CUSTO_OMITIDO, null, "Requer a ferramenta paga X", ""));

        Map<TipoAchado, Integer> pesos = new EnumMap<>(Map.of(TipoAchado.TERMO_PROIBIDO, 30,
                TipoAchado.PROMESSA_RESULTADO_SEM_RESSALVA, 20, TipoAchado.RECONHECIMENTO_SEM_COMPROVANTE, 25,
                TipoAchado.QUANTIDADE_DIVERGENTE, 15, TipoAchado.ELEMENTO_AUSENTE_NA_ENTREGA, 25,
                TipoAchado.CUSTO_OMITIDO, 30, TipoAchado.INFO_ESSENCIAL_AUSENTE, 10));
        ResultadoPontuacao r = new CalculadoraPontuacao(new PontuacaoPropriedades(30, 60, 100))
                .calcular(achados.stream().map(AchadoDetectado::tipo).toList(), pesos);

        assertThat(achados).extracting(AchadoDetectado::tipo).containsOnly(
                TipoAchado.PROMESSA_RESULTADO_SEM_RESSALVA, TipoAchado.QUANTIDADE_DIVERGENTE,
                TipoAchado.ELEMENTO_AUSENTE_NA_ENTREGA, TipoAchado.CUSTO_OMITIDO);
        assertThat(r.pontos()).isEqualTo(90);
        assertThat(r.faixa()).isEqualTo(Faixa.ALTA);
    }

    /** 28 aulas em 5 módulos, sem bônus, sem certificado; a aula 12 exige ferramenta paga. */
    private static List<ItemEntrega> entrega() {
        List<ItemEntrega> itens = new ArrayList<>();
        for (int m = 1; m <= 5; m++) {
            itens.add(new ItemEntrega(TipoConteudo.MODULO, "Módulo " + m, null));
        }
        for (int a = 1; a <= 28; a++) {
            if (a != 12) {
                itens.add(new ItemEntrega(TipoConteudo.AULA, "Aula " + a, null));
            }
        }
        itens.add(new ItemEntrega(TipoConteudo.AULA, "Aula 12", "Requer a ferramenta paga X (assinatura mensal)"));
        return itens;
    }
}
