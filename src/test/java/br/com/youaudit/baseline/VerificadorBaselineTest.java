package br.com.youaudit.baseline;

import br.com.youaudit.avaliacao.AchadoDetectado;
import br.com.youaudit.avaliacao.EntradaVerificacao;
import br.com.youaudit.avaliacao.ItemEntrega;
import br.com.youaudit.avaliacao.ResultadoVerificacao;
import br.com.youaudit.coleta.Origem;
import br.com.youaudit.coleta.TipoConteudo;
import br.com.youaudit.config.ExtracaoPropriedades;
import br.com.youaudit.extracao.PreparadorBlocos;
import br.com.youaudit.extracao.TipoAfirmacao;
import br.com.youaudit.regras.TipoAchado;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa o VerificadorBaseline com dublês do buscador, do extrator e do comparador (peças do Eduardo), para conferir
 * só a orquestração e os achados que são dele. A integração com as peças reais fica em VerificadorBaselineIntegracaoTest.
 */
class VerificadorBaselineTest {

    private static final AchadoDetectado ACHADO_DO_COMPARADOR =
            new AchadoDetectado(TipoAchado.QUANTIDADE_DIVERGENTE, "40 aulas", null, "dublê");

    private final ExtratorFake extrator = new ExtratorFake();
    private final ComparadorFake comparador = new ComparadorFake();
    private final VerificadorBaseline verificador = new VerificadorBaseline(termos(),
            new PreparadorBlocos(new ExtracaoPropriedades(1200, 8000)), new BuscadorTermosFake(), extrator, comparador);

    @Test
    void textoVazioNaoGeraNada() {
        ResultadoVerificacao r = verificador.verificar(entrada("   ", List.of()));

        assertThat(r.afirmacoes()).isEmpty();
        assertThat(r.achados()).isEmpty();
        assertThat(r.verificador()).isEqualTo("baseline");
        assertThat(r.produtoId()).isEqualTo("teste-01");
    }

    @Test
    void termoProibidoGeraAchadoComTrechoComoNaPagina() {
        List<AchadoDetectado> achados = achados("Lucro GARANTIDO todo mes.\nsuporte", List.of());

        assertThat(achados).filteredOn(a -> a.tipo() == TipoAchado.TERMO_PROIBIDO)
                .extracting(AchadoDetectado::trechoPagina).containsExactly("Lucro GARANTIDO");
    }

    @Test
    void promessaSemRessalvaPorPertoGeraAchado() {
        List<AchadoDetectado> achados = achados("Fature 10 mil por mes.\nlinha\nlinha\nresultados variam\nsuporte",
                List.of());

        assertThat(achados).filteredOn(a -> a.tipo() == TipoAchado.PROMESSA_RESULTADO_SEM_RESSALVA)
                .extracting(AchadoDetectado::trechoPagina).containsExactly("Fature 10 mil por mes.");
    }

    @Test
    void ressalvaDentroDaJanelaEvitaOAchado() {
        List<AchadoDetectado> achados = achados("Fature 10 mil por mes.\nOs resultados variam.\nsuporte", List.of());

        assertThat(achados).noneMatch(a -> a.tipo() == TipoAchado.PROMESSA_RESULTADO_SEM_RESSALVA);
    }

    @Test
    void reconhecimentoOficialSempreVaiParaConferencia() {
        List<AchadoDetectado> achados = achados("Curso reconhecido pelo MEC.\nsuporte", List.of());

        assertThat(achados).filteredOn(a -> a.tipo() == TipoAchado.RECONHECIMENTO_SEM_COMPROVANTE)
                .extracting(AchadoDetectado::trechoPagina).containsExactly("Curso reconhecido pelo MEC.");
    }

    @Test
    void semEntregaNaoCruzaNemCompara() {
        List<AchadoDetectado> achados = achados("Ganhe um bonus exclusivo.\nsuporte", List.of());

        assertThat(achados).noneMatch(a -> a.tipo() == TipoAchado.ELEMENTO_AUSENTE_NA_ENTREGA
                || a.tipo() == TipoAchado.CUSTO_OMITIDO || a.tipo() == TipoAchado.QUANTIDADE_DIVERGENTE);
        assertThat(comparador.chamadas).isZero();
    }

    @Test
    void bonusPrometidoSemItemBonusNaEntrega() {
        List<ItemEntrega> soAulas = List.of(new ItemEntrega(TipoConteudo.AULA, "Aula 1", null));

        assertThat(achados("Ganhe um bonus exclusivo.\nsuporte", soAulas))
                .filteredOn(a -> a.tipo() == TipoAchado.ELEMENTO_AUSENTE_NA_ENTREGA)
                .extracting(AchadoDetectado::trechoPagina).containsExactly("Ganhe um bonus exclusivo.");
    }

    @Test
    void bonusPrometidoComItemBonusNaoGeraAchado() {
        List<ItemEntrega> comBonus = List.of(new ItemEntrega(TipoConteudo.BONUS, "Mentoria", null));

        assertThat(achados("Ganhe um bonus exclusivo.\nsuporte", comBonus))
                .noneMatch(a -> a.tipo() == TipoAchado.ELEMENTO_AUSENTE_NA_ENTREGA);
    }

    @Test
    void custoNaEntregaQueAPaginaNaoAvisa() {
        List<ItemEntrega> itens = List.of(new ItemEntrega(TipoConteudo.AULA, "Aula 12", "Exige ferramenta paga X"));

        assertThat(achados("Curso completo.\nsuporte", itens))
                .filteredOn(a -> a.tipo() == TipoAchado.CUSTO_OMITIDO)
                .extracting(AchadoDetectado::trechoEntrega).containsExactly("Exige ferramenta paga X");
    }

    @Test
    void paginaQueAvisaDeCustoNaoGeraCustoOmitido() {
        List<ItemEntrega> itens = List.of(new ItemEntrega(TipoConteudo.AULA, "Aula 12", "Exige ferramenta paga X"));

        assertThat(achados("Algumas aulas exigem assinatura a parte.\nsuporte", itens))
                .noneMatch(a -> a.tipo() == TipoAchado.CUSTO_OMITIDO);
    }

    @Test
    void quantidadesDeTodosOsBlocosVaoParaOComparador() {
        List<ItemEntrega> itens = List.of(new ItemEntrega(TipoConteudo.AULA, "Aula 1", null));

        List<AchadoDetectado> achados = achados("bloco um\nbloco dois\nsuporte", itens);

        assertThat(extrator.textosRecebidos).containsExactly("bloco um", "bloco dois", "suporte");
        assertThat(comparador.recebido).isEqualTo(ExtratorFake.QUANTIDADES_POR_BLOCO.subList(0, 3));
        assertThat(achados).contains(ACHADO_DO_COMPARADOR);
    }

    @Test
    void informacaoEssencialAusenteNaoTemTrecho() {
        List<AchadoDetectado> achados = achados("Curso completo.", List.of());

        assertThat(achados).filteredOn(a -> a.tipo() == TipoAchado.INFO_ESSENCIAL_AUSENTE)
                .singleElement()
                .satisfies(a -> {
                    assertThat(a.trechoPagina()).isNull();
                    assertThat(a.detalhe()).contains("canal de contato");
                });
    }

    @Test
    void trechosDaPaginaSaoSempreLiterais() {
        String texto = "Lucro garantido!  Fature 10 mil por mes.\nReconhecido pelo MEC. Ganhe bonus.\nsuporte";
        ResultadoVerificacao r = verificador.verificar(entrada(texto,
                List.of(new ItemEntrega(TipoConteudo.AULA, "Aula 1", null))));

        r.afirmacoes().forEach(a -> assertThat(texto).contains(a.trecho()));
        r.achados().stream().filter(a -> a.trechoPagina() != null && !a.equals(ACHADO_DO_COMPARADOR))
                .forEach(a -> assertThat(texto).contains(a.trechoPagina()));
    }

    private List<AchadoDetectado> achados(String texto, List<ItemEntrega> itens) {
        return verificador.verificar(entrada(texto, itens)).achados();
    }

    private static EntradaVerificacao entrada(String texto, List<ItemEntrega> itens) {
        return new EntradaVerificacao("teste-01", Origem.SIMULADO, texto, new BigDecimal("297"), itens);
    }

    private static TermosBaseline termos() {
        Map<TipoAfirmacao, List<String>> palavras = new EnumMap<>(TipoAfirmacao.class);
        palavras.put(TipoAfirmacao.RESULTADO_FINANCEIRO, List.of("fature", "por mes"));
        palavras.put(TipoAfirmacao.RECONHECIMENTO_OFICIAL, List.of("reconhecido pelo mec"));
        palavras.put(TipoAfirmacao.BONUS, List.of("bonus"));
        palavras.put(TipoAfirmacao.CUSTO_ADICIONAL, List.of("assinatura"));
        Map<String, List<String>> essenciais = new LinkedHashMap<>();
        essenciais.put("canal de contato", List.of("suporte"));
        return new TermosBaseline("teste", List.of("lucro garantido"), palavras, List.of("resultados variam"), 1,
                List.of("paga"), essenciais, Map.of(UnidadeQuantidade.AULA, List.of("aulas"),
                UnidadeQuantidade.MODULO, List.of("modulos")));
    }

    /** Devolve uma quantidade fixa por bloco e anota os textos recebidos. */
    private static final class ExtratorFake extends ExtratorNumeros {
        static final List<QuantidadeEncontrada> QUANTIDADES_POR_BLOCO = List.of(
                new QuantidadeEncontrada(1, UnidadeQuantidade.AULA, Comparacao.EXATO, "1 aula"),
                new QuantidadeEncontrada(2, UnidadeQuantidade.AULA, Comparacao.EXATO, "2 aulas"),
                new QuantidadeEncontrada(3, UnidadeQuantidade.AULA, Comparacao.EXATO, "3 aulas"));
        final List<String> textosRecebidos = new ArrayList<>();

        ExtratorFake() {
            super(Map.of());
        }

        @Override
        public List<QuantidadeEncontrada> extrair(String texto) {
            textosRecebidos.add(texto);
            return List.of(QUANTIDADES_POR_BLOCO.get(textosRecebidos.size() - 1));
        }
    }

    /** Anota o que recebeu e devolve um achado fixo. */
    private static final class ComparadorFake extends ComparadorQuantidades {
        int chamadas;
        List<QuantidadeEncontrada> recebido;

        @Override
        public List<AchadoDetectado> comparar(List<QuantidadeEncontrada> naPagina, List<ItemEntrega> entrega) {
            chamadas++;
            recebido = naPagina;
            return List.of(ACHADO_DO_COMPARADOR);
        }
    }
}
