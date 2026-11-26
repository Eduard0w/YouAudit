package br.com.youaudit.entrega;

import br.com.youaudit.avaliacao.AchadoDetectado;
import br.com.youaudit.avaliacao.AfirmacaoDetectada;
import br.com.youaudit.coleta.Origem;
import br.com.youaudit.coleta.Produto;
import br.com.youaudit.coleta.ProdutoRepository;
import br.com.youaudit.config.RelogioConfig;
import br.com.youaudit.evento.Auditoria;
import br.com.youaudit.evento.AuditoriaRepository;
import br.com.youaudit.evento.CalculadoraHash;
import br.com.youaudit.evento.EntradaAuditoria;
import br.com.youaudit.extracao.CacheRespostasIa;
import br.com.youaudit.extracao.ClienteIaFake;
import br.com.youaudit.extracao.TipoAfirmacao;
import br.com.youaudit.extracao.ValidadorTrechoLiteral;
import br.com.youaudit.regras.TipoAchado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;

import static br.com.youaudit.entrega.AnalisadorRespostaCruzamentoTest.ITENS;
import static org.assertj.core.api.Assertions.assertThat;

/** A IA é o ClienteIaFake; o resto (cache, banco, validação) é de verdade. */
@DataJpaTest
@Import({CacheRespostasIa.class, CalculadoraHash.class, RelogioConfig.class})
class ServicoCruzamentoTest {

    private static final List<AfirmacaoDetectada> AFIRMACOES = List.of(
            new AfirmacaoDetectada(3, TipoAfirmacao.BONUS, "bônus mentoria"),
            new AfirmacaoDetectada(4, TipoAfirmacao.GARANTIA, "garantia de 7 dias"));

    @Autowired private CacheRespostasIa cache;
    @Autowired private DescarteCruzamentoRepository descartes;
    @Autowired private ProdutoRepository produtos;
    @Autowired private AuditoriaRepository auditorias;

    private ClienteIaFake ia;
    private Auditoria auditoria;

    @BeforeEach
    void preparar() {
        ia = new ClienteIaFake();
        Produto produto = new Produto("sim-001");
        produto.setNome("sim-001");
        produto.setOrigem(Origem.SIMULADO);
        produtos.save(produto);
        auditoria = auditorias.save(new Auditoria(produto, 1, "a".repeat(64), EntradaAuditoria.EVENTO, Instant.now()));
    }

    private ServicoCruzamento servico() {
        return new ServicoCruzamento(ia, cache, new AnalisadorRespostaCruzamento(new ValidadorTrechoLiteral()), descartes);
    }

    @Test
    void semEntregaPulaESemChamarAIa() {
        ResultadoCruzamento r = servico().cruzar(auditoria, AFIRMACOES, List.of());

        assertThat(r.pulado()).isTrue();
        assertThat(ia.pedidos()).isEmpty();
    }

    @Test
    void bonusSemItemECustoNaEntregaViramAchados() {
        ia.responder("""
                {"associacoes":[{"promessa":0,"item":null}],
                 "custos":[{"item":1,"trecho_entrega":"Requer assinatura da ferramenta X (paga)"}]}""");

        ResultadoCruzamento r = servico().cruzar(auditoria, AFIRMACOES, ITENS);

        assertThat(r.achados()).extracting(AchadoDetectado::tipo)
                .containsExactly(TipoAchado.ELEMENTO_AUSENTE_NA_ENTREGA, TipoAchado.CUSTO_OMITIDO);
        assertThat(r.achados().get(0).trechoPagina()).isEqualTo("bônus mentoria");
        assertThat(r.achados().get(1).trechoEntrega()).isEqualTo("Requer assinatura da ferramenta X (paga)");
        assertThat(r.chamadasIa()).isEqualTo(1);
    }

    @Test
    void paginaQueAvisaCustoNaoGeraCustoOmitido() {
        ia.responder("""
                {"associacoes":[{"promessa":0,"item":2,"trecho_entrega":"Mentoria em grupo"}],
                 "custos":[{"item":1,"trecho_entrega":"Requer assinatura"}]}""");
        List<AfirmacaoDetectada> comAviso = List.of(AFIRMACOES.get(0),
                new AfirmacaoDetectada(5, TipoAfirmacao.CUSTO_ADICIONAL, "ferramenta paga à parte"));

        assertThat(servico().cruzar(auditoria, comAviso, ITENS).achados()).isEmpty();
    }

    @Test
    void trechoInventadoTemNovaTentativaEFicaRegistrado() {
        ia.responder("""
                {"associacoes":[{"promessa":0,"item":2,"trecho_entrega":"mentoria individual"}],"custos":[]}""")
          .responder("""
                {"associacoes":[{"promessa":0,"item":2,"trecho_entrega":"Mentoria em grupo"}],"custos":[]}""");

        ResultadoCruzamento r = servico().cruzar(auditoria, AFIRMACOES, ITENS);

        assertThat(r.chamadasIa()).isEqualTo(2);
        assertThat(r.achados()).isEmpty(); // a segunda resposta associou o bônus a um item real
        assertThat(descartes.findByAuditoriaId(auditoria.getId())).hasSize(1);
        assertThat(ia.pedidos().get(1).conteudo()).contains("trecho não encontrado literalmente no item");
    }

    @Test
    void segundaFalhaTambemDescartaENaoInventaAchado() {
        ia.responder("{\"associacoes\":[],\"custos\":[]}").responder("{\"associacoes\":[],\"custos\":[]}");

        ResultadoCruzamento r = servico().cruzar(auditoria, AFIRMACOES, ITENS);

        assertThat(r.achados()).isEmpty(); // promessa sem resposta não vira "ausente"
        assertThat(r.descartes()).isEqualTo(2);
    }

    @Test
    void respostaValidaVaiParaOCacheEASegundaExecucaoNaoChamaAIa() {
        ia.responder("{\"associacoes\":[{\"promessa\":0,\"item\":null}],\"custos\":[]}");
        servico().cruzar(auditoria, AFIRMACOES, ITENS);

        ResultadoCruzamento r = servico().cruzar(auditoria, AFIRMACOES, ITENS);

        assertThat(r.chamadasIa()).isZero();
        assertThat(r.respostasDoCache()).isEqualTo(1);
        assertThat(r.achados()).hasSize(1);
    }
}
