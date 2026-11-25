package br.com.youaudit.evento;

import br.com.youaudit.coleta.ConteudoItemRepository;
import br.com.youaudit.coleta.LimpadorTexto;
import br.com.youaudit.coleta.Origem;
import br.com.youaudit.coleta.ProdutoRepository;
import br.com.youaudit.coleta.TipoConteudo;
import br.com.youaudit.config.RelogioConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testa a idempotência por hash com um banco H2 de verdade (@DataJpaTest).
 * Cada teste roda numa transação que é desfeita no fim, então um teste não suja o outro.
 */
@DataJpaTest
@Import({ServicoEvento.class, LimpadorTexto.class, CalculadoraHash.class, RelogioConfig.class})
class ServicoEventoTest {

    private static final String PAGINA_A = "<p>Método Loja Lucrativa</p><p>40 aulas</p><p>Apenas R$ 297</p>";
    private static final String PAGINA_B = "<p>Método Loja Lucrativa</p><p>28 aulas</p><p>Apenas R$ 297</p>";

    @Autowired private ServicoEvento servico;
    @Autowired private AuditoriaRepository auditorias;
    @Autowired private EventoProdutoRepository eventos;
    @Autowired private ProdutoRepository produtos;
    @Autowired private ConteudoItemRepository conteudos;

    @Test
    void primeiroEnvioCriaAuditoriaVersao1EmAnalise() {
        EventoResposta resposta = servico.receber(evento("sim-001", PAGINA_A, null));

        assertThat(resposta.resultado()).isEqualTo(ResultadoEvento.PROCESSADO);
        assertThat(resposta.versao()).isEqualTo(1);
        Auditoria auditoria = auditorias.findById(resposta.auditoriaId()).orElseThrow();
        assertThat(auditoria.getStatus()).isEqualTo(StatusAuditoria.EM_ANALISE);
        assertThat(produtos.findById("sim-001").orElseThrow().getPaginaTexto())
                .isEqualTo("Método Loja Lucrativa\n40 aulas\nApenas R$ 297");
    }

    @Test
    void mesmoEventoDuasVezesEhIgnoradoNaSegunda() {
        servico.receber(evento("sim-001", PAGINA_A, null));
        EventoResposta segunda = servico.receber(evento("sim-001", PAGINA_A, null));

        assertThat(segunda.resultado()).isEqualTo(ResultadoEvento.IGNORADO_MESMO_HASH);
        assertThat(segunda.auditoriaId()).isNull();
        assertThat(auditorias.findByProdutoIdOrderByVersaoAsc("sim-001")).hasSize(1);
        // O evento ignorado também fica registrado na trilha.
        assertThat(eventos.findByProdutoIdOrderByIdAsc("sim-001"))
                .extracting(EventoProduto::getResultado)
                .containsExactly(ResultadoEvento.PROCESSADO, ResultadoEvento.IGNORADO_MESMO_HASH);
    }

    @Test
    void mudarSoHtmlOuEspacosNaoContaComoPaginaNova() {
        servico.receber(evento("sim-001", PAGINA_A, null));
        String mesmaPaginaOutroHtml = "<div>Método   Loja Lucrativa</div>\n<div>40 aulas</div><br><b>Apenas R$ 297</b>";

        EventoResposta resposta = servico.receber(evento("sim-001", mesmaPaginaOutroHtml, null));

        assertThat(resposta.resultado()).isEqualTo(ResultadoEvento.IGNORADO_MESMO_HASH);
    }

    @Test
    void paginaAlteradaGeraNovaVersao() {
        servico.receber(evento("sim-001", PAGINA_A, null));
        EventoResposta resposta = servico.receber(evento("sim-001", PAGINA_B, null));

        assertThat(resposta.resultado()).isEqualTo(ResultadoEvento.PROCESSADO);
        assertThat(resposta.versao()).isEqualTo(2);
        assertThat(produtos.findById("sim-001").orElseThrow().getVersaoAtual()).isEqualTo(2);
    }

    @Test
    void paginaQueVoltaAoTextoAnteriorEhAuditadaDeNovo() {
        servico.receber(evento("sim-001", PAGINA_A, null));
        servico.receber(evento("sim-001", PAGINA_B, null));
        EventoResposta resposta = servico.receber(evento("sim-001", PAGINA_A, null));

        assertThat(resposta.resultado()).isEqualTo(ResultadoEvento.PROCESSADO);
        assertThat(resposta.versao()).isEqualTo(3);
    }

    @Test
    void auditoriaQueFalhouNaoBloqueiaReenvioDaMesmaPagina() {
        EventoResposta primeira = servico.receber(evento("sim-001", PAGINA_A, null));
        Auditoria auditoria = auditorias.findById(primeira.auditoriaId()).orElseThrow();
        auditoria.setStatus(StatusAuditoria.FALHOU);

        EventoResposta resposta = servico.receber(evento("sim-001", PAGINA_A, null));

        assertThat(resposta.resultado()).isEqualTo(ResultadoEvento.PROCESSADO);
        assertThat(resposta.versao()).isEqualTo(2);
    }

    @Test
    void produtosDiferentesComMesmaPaginaNaoSeAfetam() {
        servico.receber(evento("sim-001", PAGINA_A, null));
        EventoResposta outro = servico.receber(evento("sim-002", PAGINA_A, null));

        assertThat(outro.resultado()).isEqualTo(ResultadoEvento.PROCESSADO);
        assertThat(outro.versao()).isEqualTo(1);
    }

    @Test
    void novaVersaoSubstituiAEntrega() {
        servico.receber(evento("sim-001", PAGINA_A, new ConteudoDoEvento(List.of(
                new ItemDoEvento(TipoConteudo.MODULO, "Módulo 1", null, null),
                new ItemDoEvento(TipoConteudo.AULA, "Aula 1", "Introdução", 10)))));
        servico.receber(evento("sim-001", PAGINA_B, new ConteudoDoEvento(List.of(
                new ItemDoEvento(TipoConteudo.BONUS, "Mentoria", null, null)))));

        assertThat(conteudos.findByProdutoId("sim-001"))
                .extracting(c -> c.getTitulo())
                .containsExactly("Mentoria");
    }

    @Test
    void paginaSoComTagsEhRecusada() {
        assertThatThrownBy(() -> servico.receber(evento("sim-001", "<div><p></p></div>", null)))
                .isInstanceOf(PaginaVaziaException.class);
    }

    private static EventoProdutoRequest evento(String id, String pagina, ConteudoDoEvento conteudo) {
        return new EventoProdutoRequest(TipoEvento.PRODUTO_ENVIADO, new ProdutoDoEvento(
                id, "Método Loja Lucrativa", "curso", new BigDecimal("297.00"), Origem.SIMULADO, pagina, conteudo));
    }
}
