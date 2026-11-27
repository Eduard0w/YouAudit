package br.com.youaudit.revisao;

import br.com.youaudit.coleta.Origem;
import br.com.youaudit.coleta.Produto;
import br.com.youaudit.coleta.ProdutoRepository;
import br.com.youaudit.coleta.StatusProduto;
import br.com.youaudit.evento.Auditoria;
import br.com.youaudit.evento.AuditoriaRepository;
import br.com.youaudit.evento.CalculadoraHash;
import br.com.youaudit.evento.EntradaAuditoria;
import br.com.youaudit.evento.StatusAuditoria;
import br.com.youaudit.extracao.Afirmacao;
import br.com.youaudit.extracao.AfirmacaoRepository;
import br.com.youaudit.extracao.TipoAfirmacao;
import br.com.youaudit.regras.Achado;
import br.com.youaudit.regras.AchadoRepository;
import br.com.youaudit.regras.EstadoAchado;
import br.com.youaudit.regras.Severidade;
import br.com.youaudit.regras.TipoAchado;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Peça 3: o LIBERAR congela as promessas; depois, o registro não muda e os sinais são recalculados. */
@DataJpaTest
@Import({ServicoRevisao.class, ServicoRegistroPromessas.class, CalculadoraHash.class, JacksonAutoConfiguration.class,
        ServicoRegistroPromessasTest.Relogio.class})
class ServicoRegistroPromessasTest {

    private static final Instant AGORA = Instant.parse("2026-11-27T10:00:00.123456789Z"); // com nanossegundos de propósito
    private static final String HASH_V1 = "a".repeat(64);

    @TestConfiguration
    static class Relogio {
        @Bean
        Clock relogio() {
            return Clock.fixed(AGORA, ZoneOffset.UTC);
        }
    }

    @Autowired private ServicoRevisao revisao;
    @Autowired private ServicoRegistroPromessas servico;
    @Autowired private ProdutoRepository produtos;
    @Autowired private AuditoriaRepository auditorias;
    @Autowired private AfirmacaoRepository afirmacoes;
    @Autowired private AchadoRepository achados;
    @Autowired private RegistroPromessasRepository registros;
    @Autowired private CalculadoraHash hash;

    @Test
    void liberarCongelaAsPromessasEOsAlertasComOJulgamentoDaPessoa() {
        Produto p = produto();
        Auditoria a = auditoria(p, StatusAuditoria.CONCLUIDA);
        afirmacoes.save(new Afirmacao(a, 2, TipoAfirmacao.ACESSO, "Acesso por 12 meses", "12 meses", 0.9));
        afirmacoes.save(new Afirmacao(a, 1, TipoAfirmacao.QUANTIDADE_CONTEUDO, "28 aulas", "28", 0.95));
        Achado alerta = achados.save(new Achado(a, null, TipoAchado.TERMO_PROIBIDO, Severidade.ALTA, "Dinheiro fácil", null));
        revisao.revisar(alerta.getId(), new PedidoRevisao(DecisaoRevisao.DESCARTADO, "expressão de marketing aceitável"));

        revisao.decidir("sim-001", new PedidoDecisao(DecisaoProduto.LIBERAR, "  página coerente com a entrega "));

        List<RegistroPromessasVisto> lista = servico.listar("sim-001");
        assertThat(lista).singleElement().satisfies(r -> {
            assertThat(r.versao()).isEqualTo(1);
            assertThat(r.motivo()).isEqualTo("página coerente com a entrega");
            // cortada em microssegundos, e igual dentro e fora do conteúdo congelado
            assertThat(r.data()).isEqualTo(AGORA.truncatedTo(ChronoUnit.MICROS));
            assertThat(r.conteudo().data()).isEqualTo(r.data().toString());
            assertThat(r.hashPagina()).isEqualTo(HASH_V1);
            assertThat(r.integro()).isTrue();
            assertThat(r.paginaMudouDesdeAprovacao()).isFalse();
            assertThat(r.conteudo().aviso()).isNull();
            assertThat(r.conteudo().origem()).isEqualTo(Origem.SIMULADO);
            assertThat(r.conteudo().promessas()).extracting(RegistroPromessasVisto.Promessa::trecho)
                    .containsExactly("28 aulas", "Acesso por 12 meses");   // ordem dos blocos da página
            assertThat(r.conteudo().alertas()).singleElement().satisfies(x -> {
                assertThat(x.estado()).isEqualTo(EstadoAchado.DESCARTADO);
                assertThat(x.nota()).isEqualTo("expressão de marketing aceitável");
            });
        });
    }

    @Test
    void soOLiberarCriaRegistro() {
        Produto p = produto();
        auditoria(p, StatusAuditoria.CONCLUIDA);

        revisao.decidir("sim-001", new PedidoDecisao(DecisaoProduto.PEDIR_AJUSTE, "corrigir"));
        revisao.decidir("sim-001", new PedidoDecisao(DecisaoProduto.ESCALAR, "dúvida"));

        assertThat(servico.listar("sim-001")).isEmpty();
    }

    @Test
    void revisarUmAlertaDepoisDoLiberarNaoMudaORegistro() {
        Produto p = produto();
        Auditoria a = auditoria(p, StatusAuditoria.CONCLUIDA);
        Achado alerta = achados.save(new Achado(a, null, TipoAchado.TERMO_PROIBIDO, Severidade.ALTA, "Dinheiro fácil", null));
        revisao.revisar(alerta.getId(), new PedidoRevisao(DecisaoRevisao.DESCARTADO, "aceitável"));
        revisao.decidir("sim-001", new PedidoDecisao(DecisaoProduto.LIBERAR, "ok"));

        revisao.revisar(alerta.getId(), new PedidoRevisao(DecisaoRevisao.CONFIRMADO, "mudei de ideia"));

        RegistroPromessasVisto r = servico.listar("sim-001").get(0);
        assertThat(r.conteudo().alertas().get(0).estado()).isEqualTo(EstadoAchado.DESCARTADO);
        assertThat(r.conteudo().alertas().get(0).nota()).isEqualTo("aceitável");
        assertThat(r.integro()).isTrue();
    }

    @Test
    void conteudoAlteradoDepoisDeGravadoNaoEIntegro() {
        Produto p = produto();
        String original = "{\"produtoId\":\"sim-001\"}";
        RegistroPromessas adulterado = registros.save(new RegistroPromessas(p, 1, HASH_V1, "ok", AGORA,
                "{\"produtoId\":\"sim-999\"}", hash.sha256(original)));

        assertThat(servico.ver(adulterado, p).integro()).isFalse();
    }

    @Test
    void paginaNovaDepoisDaAprovacaoApareceComoMudanca() {
        Produto p = produto();
        auditoria(p, StatusAuditoria.CONCLUIDA);
        revisao.decidir("sim-001", new PedidoDecisao(DecisaoProduto.LIBERAR, "ok"));

        p.setPaginaHash("b".repeat(64));   // o que um reenvio com outro texto faz

        assertThat(servico.listar("sim-001").get(0).paginaMudouDesdeAprovacao()).isTrue();
    }

    @Test
    void liberarComAuditoriaQueFalhouRegistraOAviso() {
        Produto p = produto();
        auditoria(p, StatusAuditoria.FALHOU);

        revisao.decidir("sim-001", new PedidoDecisao(DecisaoProduto.LIBERAR, "liberado sem IA"));

        RegistroPromessasVisto r = servico.listar("sim-001").get(0);
        assertThat(r.conteudo().aviso()).isEqualTo("auditoria FALHOU: promessas não extraídas");
        assertThat(r.conteudo().promessas()).isEmpty();
        assertThat(r.integro()).isTrue();
    }

    private Produto produto() {
        Produto p = new Produto("sim-001");
        p.setNome("Método Loja Lucrativa");
        p.setOrigem(Origem.SIMULADO);
        p.setStatus(StatusProduto.AGUARDANDO_DECISAO);
        p.setPaginaHash(HASH_V1);
        p.setVersaoAtual(1);
        return produtos.save(p);
    }

    private Auditoria auditoria(Produto p, StatusAuditoria status) {
        Auditoria a = new Auditoria(p, 1, HASH_V1, EntradaAuditoria.EVENTO, AGORA.minusSeconds(600));
        a.setStatus(status);
        a.setPontuacao(status == StatusAuditoria.CONCLUIDA ? 30 : null);
        return auditorias.save(a);
    }
}
