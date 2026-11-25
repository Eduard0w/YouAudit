package br.com.youaudit.evento;

import br.com.youaudit.coleta.AtribuidorIdLink;
import br.com.youaudit.coleta.ColetorPagina;
import br.com.youaudit.coleta.ConteudoItemRepository;
import br.com.youaudit.coleta.GravadorSnapshot;
import br.com.youaudit.coleta.LeitorRobots;
import br.com.youaudit.coleta.LimitadorRequisicoes;
import br.com.youaudit.coleta.LimpadorTexto;
import br.com.youaudit.coleta.Origem;
import br.com.youaudit.coleta.Produto;
import br.com.youaudit.coleta.ProdutoRepository;
import br.com.youaudit.coleta.ResolvedorDnsSistema;
import br.com.youaudit.coleta.ValidadorDestino;
import br.com.youaudit.coleta.ServidorHttpLocal;
import br.com.youaudit.coleta.StatusProduto;
import br.com.youaudit.coleta.TipoConteudo;
import br.com.youaudit.config.ColetaConfig;
import br.com.youaudit.config.RelogioConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static br.com.youaudit.coleta.ServidorHttpLocal.recurso;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testa os passos 1 e 2 (evento, coleta, hash e idempotência) com um banco H2 de verdade (@DataJpaTest)
 * e, nos casos com URL, uma coleta de verdade contra um servidor HTTP local.
 * Cada teste roda numa transação desfeita no fim, então um teste não suja o outro no banco.
 */
@DataJpaTest
@Import({ServicoEvento.class, LimpadorTexto.class, CalculadoraHash.class, RelogioConfig.class, ColetaConfig.class,
        ColetorPagina.class, LeitorRobots.class, LimitadorRequisicoes.class, GravadorSnapshot.class,
        ValidadorDestino.class, ResolvedorDnsSistema.class, AtribuidorIdLink.class})
class ServicoEventoTest {

    private static final String PAGINA_A = "<p>Método Loja Lucrativa</p><p>40 aulas</p><p>Apenas R$ 297</p>";
    private static final String PAGINA_B = "<p>Método Loja Lucrativa</p><p>28 aulas</p><p>Apenas R$ 297</p>";

    private static ServidorHttpLocal servidor;
    private static Path pastaSnapshots;

    @Autowired private ServicoEvento servico;
    @Autowired private AuditoriaRepository auditorias;
    @Autowired private EventoProdutoRepository eventos;
    @Autowired private ProdutoRepository produtos;
    @Autowired private ConteudoItemRepository conteudos;

    @BeforeAll
    static void iniciarServidor() {
        servidor = ServidorHttpLocal.iniciar()
                .robots("User-agent: *\nDisallow:")
                .html("/longa", recurso("/paginas/pagina-longa.html"))
                .html("/curta", recurso("/paginas/pagina-curta.html"))
                .html("/outra", recurso("/paginas/pagina-longa.html").replace("40 aulas", "28 aulas"));
    }

    @AfterAll
    static void desligarServidor() {
        servidor.close();
    }

    /** Snapshots numa pasta temporária (não em data/real) e sem a espera de 1 s entre requisições. */
    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registro) throws IOException {
        pastaSnapshots = Files.createTempDirectory("youaudit-snapshots-teste");
        registro.add("coleta.diretorio-snapshots", pastaSnapshots::toString);
        registro.add("coleta.intervalo-minimo-ms", () -> "0");
        // O servidor de teste roda em 127.0.0.1, que a proteção contra SSRF bloqueia: liberado só aqui.
        registro.add("coleta.ips-liberados-para-teste", () -> "127.0.0.1");
    }

    // ---------- pagina_texto ----------

    @Test
    void primeiroEnvioCriaAuditoriaVersao1EmAnalise() {
        EventoResposta resposta = servico.receber(eventoComTexto("sim-001", PAGINA_A, null));

        assertThat(resposta.resultado()).isEqualTo(ResultadoEvento.PROCESSADO);
        assertThat(resposta.versao()).isEqualTo(1);
        assertThat(resposta.statusAuditoria()).isEqualTo(StatusAuditoria.EM_ANALISE);
        assertThat(produtos.findById("sim-001").orElseThrow().getPaginaTexto())
                .isEqualTo("Método Loja Lucrativa\n40 aulas\nApenas R$ 297");
    }

    @Test
    void mesmoEventoDuasVezesEhIgnoradoNaSegunda() {
        servico.receber(eventoComTexto("sim-001", PAGINA_A, null));
        EventoResposta segunda = servico.receber(eventoComTexto("sim-001", PAGINA_A, null));

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
        servico.receber(eventoComTexto("sim-001", PAGINA_A, null));
        String mesmaPaginaOutroHtml = "<div>Método   Loja Lucrativa</div>\n<div>40 aulas</div><br><b>Apenas R$ 297</b>";

        EventoResposta resposta = servico.receber(eventoComTexto("sim-001", mesmaPaginaOutroHtml, null));

        assertThat(resposta.resultado()).isEqualTo(ResultadoEvento.IGNORADO_MESMO_HASH);
    }

    @Test
    void paginaAlteradaGeraNovaVersao() {
        servico.receber(eventoComTexto("sim-001", PAGINA_A, null));
        EventoResposta resposta = servico.receber(eventoComTexto("sim-001", PAGINA_B, null));

        assertThat(resposta.resultado()).isEqualTo(ResultadoEvento.PROCESSADO);
        assertThat(resposta.versao()).isEqualTo(2);
        assertThat(produtos.findById("sim-001").orElseThrow().getVersaoAtual()).isEqualTo(2);
    }

    @Test
    void paginaQueVoltaAoTextoAnteriorEhAuditadaDeNovo() {
        servico.receber(eventoComTexto("sim-001", PAGINA_A, null));
        servico.receber(eventoComTexto("sim-001", PAGINA_B, null));
        EventoResposta resposta = servico.receber(eventoComTexto("sim-001", PAGINA_A, null));

        assertThat(resposta.resultado()).isEqualTo(ResultadoEvento.PROCESSADO);
        assertThat(resposta.versao()).isEqualTo(3);
    }

    @Test
    void auditoriaQueFalhouNaoBloqueiaReenvioDaMesmaPagina() {
        EventoResposta primeira = servico.receber(eventoComTexto("sim-001", PAGINA_A, null));
        auditorias.findById(primeira.auditoriaId()).orElseThrow().setStatus(StatusAuditoria.FALHOU);

        EventoResposta resposta = servico.receber(eventoComTexto("sim-001", PAGINA_A, null));

        assertThat(resposta.resultado()).isEqualTo(ResultadoEvento.PROCESSADO);
        assertThat(resposta.versao()).isEqualTo(2);
    }

    @Test
    void produtosDiferentesComMesmaPaginaNaoSeAfetam() {
        servico.receber(eventoComTexto("sim-001", PAGINA_A, null));
        EventoResposta outro = servico.receber(eventoComTexto("sim-002", PAGINA_A, null));

        assertThat(outro.resultado()).isEqualTo(ResultadoEvento.PROCESSADO);
        assertThat(outro.versao()).isEqualTo(1);
    }

    @Test
    void novaVersaoSubstituiAEntrega() {
        servico.receber(eventoComTexto("sim-001", PAGINA_A, new ConteudoDoEvento(List.of(
                new ItemDoEvento(TipoConteudo.MODULO, "Módulo 1", null, null),
                new ItemDoEvento(TipoConteudo.AULA, "Aula 1", "Introdução", 10)))));
        servico.receber(eventoComTexto("sim-001", PAGINA_B, new ConteudoDoEvento(List.of(
                new ItemDoEvento(TipoConteudo.BONUS, "Mentoria", null, null)))));

        assertThat(conteudos.findByProdutoId("sim-001"))
                .extracting(c -> c.getTitulo())
                .containsExactly("Mentoria");
    }

    @Test
    void paginaSoComTagsEhRecusada() {
        assertThatThrownBy(() -> servico.receber(eventoComTexto("sim-001", "<div><p></p></div>", null)))
                .isInstanceOf(PaginaVaziaException.class);
    }

    @Test
    void textoCurtoPorPaginaTextoNaoViraColetaInsuficiente() {
        // O mínimo de 500 caracteres vale só para URL: as páginas fictícias podem ser curtas.
        EventoResposta resposta = servico.receber(eventoComTexto("sim-001", PAGINA_A, null));

        assertThat(resposta.statusAuditoria()).isEqualTo(StatusAuditoria.EM_ANALISE);
    }

    // ---------- url ----------

    @Test
    void urlComPaginaLongaUsaOTextoDoSnapshot() throws IOException {
        EventoResposta resposta = servico.receber(eventoComUrl("pagina-01", servidor.url("/longa"), null));

        assertThat(resposta.statusAuditoria()).isEqualTo(StatusAuditoria.EM_ANALISE);
        Produto produto = produtos.findById("pagina-01").orElseThrow();
        String textoDoSnapshot = Files.readString(ultimoSnapshot("pagina-01").resolve("texto.txt"));
        assertThat(produto.getPaginaTexto()).isEqualTo(textoDoSnapshot);
        assertThat(produto.getPaginaHash()).isEqualTo(new CalculadoraHash().sha256(textoDoSnapshot));
        assertThat(auditorias.findById(resposta.auditoriaId()).orElseThrow().getHashPagina())
                .isEqualTo(produto.getPaginaHash());
    }

    @Test
    void urlComTextoCurtoViraColetaInsuficiente() {
        EventoResposta resposta = servico.receber(eventoComUrl("pagina-02", servidor.url("/curta"), null));

        assertThat(resposta.statusAuditoria()).isEqualTo(StatusAuditoria.COLETA_INSUFICIENTE);
        Auditoria auditoria = auditorias.findById(resposta.auditoriaId()).orElseThrow();
        assertThat(auditoria.getMotivoFalha()).contains("mínimo configurado: 500");
        assertThat(auditoria.getConcluidaEm()).isNotNull();
        assertThat(produtos.findById("pagina-02").orElseThrow().getStatus())
                .isEqualTo(StatusProduto.AGUARDANDO_DECISAO);
    }

    @Test
    void coletaInsuficienteNaoBloqueiaReenvio() {
        servico.receber(eventoComUrl("pagina-02", servidor.url("/curta"), null));
        EventoResposta segunda = servico.receber(eventoComUrl("pagina-02", servidor.url("/curta"), null));

        assertThat(segunda.resultado()).isEqualTo(ResultadoEvento.PROCESSADO);
        assertThat(segunda.versao()).isEqualTo(2);
    }

    @Test
    void mesmaUrlComMesmaPaginaEhIgnorada() {
        servico.receber(eventoComUrl("pagina-01", servidor.url("/longa"), null));
        EventoResposta segunda = servico.receber(eventoComUrl("pagina-01", servidor.url("/longa"), null));

        assertThat(segunda.resultado()).isEqualTo(ResultadoEvento.IGNORADO_MESMO_HASH);
    }

    @Test
    void urlQueFalhaViraAuditoriaFalhouSemHashESemUrlNoMotivo() {
        EventoResposta resposta = servico.receber(eventoComUrl("pagina-03", servidor.url("/nao-existe"), null));

        assertThat(resposta.statusAuditoria()).isEqualTo(StatusAuditoria.FALHOU);
        Auditoria auditoria = auditorias.findById(resposta.auditoriaId()).orElseThrow();
        assertThat(auditoria.getHashPagina()).isNull();
        assertThat(auditoria.getMotivoFalha()).contains("HTTP 404").doesNotContain("127.0.0.1");
        assertThat(produtos.findById("pagina-03").orElseThrow().getStatus())
                .isEqualTo(StatusProduto.AGUARDANDO_DECISAO);
    }

    @Test
    void quandoVemUrlETextoValeAUrl() {
        ProdutoDoEvento dados = new ProdutoDoEvento("pagina-01", "Produto", "curso", new BigDecimal("297.00"),
                Origem.REAL, servidor.url("/longa"), "texto que deve ser ignorado", null);

        servico.receber(new EventoProdutoRequest(TipoEvento.PRODUTO_ENVIADO, dados));

        assertThat(produtos.findById("pagina-01").orElseThrow().getPaginaTexto())
                .doesNotContain("texto que deve ser ignorado")
                .contains("Método Loja Lucrativa");
    }

    // ---------- entrada ----------

    @Test
    void eventoSemEntradaFicaComEtiquetaEvento() {
        EventoResposta resposta = servico.receber(eventoComTexto("sim-001", PAGINA_A, null));

        assertThat(resposta.entrada()).isEqualTo(EntradaAuditoria.EVENTO);
        assertThat(auditorias.findById(resposta.auditoriaId()).orElseThrow().getEntrada())
                .isEqualTo(EntradaAuditoria.EVENTO);
    }

    @Test
    void linkRecebeIdAnonimoEEtiquetaLink() {
        EventoResposta resposta = servico.receber(eventoLink(servidor.url("/longa")));

        assertThat(resposta.produtoId()).isEqualTo("link-01");
        assertThat(resposta.entrada()).isEqualTo(EntradaAuditoria.LINK);
        Produto produto = produtos.findById("link-01").orElseThrow();
        assertThat(produto.getNome()).isEqualTo("link-01");
        assertThat(produto.getPreco()).isNull();
        assertThat(auditorias.findById(resposta.auditoriaId()).orElseThrow().getEntrada())
                .isEqualTo(EntradaAuditoria.LINK);
    }

    @Test
    void mesmoLinkDuasVezesReusaOIdEEhIgnorado() {
        servico.receber(eventoLink(servidor.url("/longa")));
        EventoResposta segunda = servico.receber(eventoLink(servidor.url("/longa")));

        assertThat(segunda.produtoId()).isEqualTo("link-01");
        assertThat(segunda.resultado()).isEqualTo(ResultadoEvento.IGNORADO_MESMO_HASH);
    }

    @Test
    void linkDiferenteRecebeProximoNumero() {
        servico.receber(eventoLink(servidor.url("/longa")));
        EventoResposta outro = servico.receber(eventoLink(servidor.url("/outra")));

        assertThat(outro.produtoId()).isEqualTo("link-02");
    }

    @Test
    void bancoGuardaOHashDaUrlENuncaAUrl() {
        String url = servidor.url("/longa");
        servico.receber(eventoLink(url));

        Produto produto = produtos.findById("link-01").orElseThrow();
        assertThat(produto.getUrlHash()).isEqualTo(new CalculadoraHash().sha256(url));
        assertThat(produto.getPaginaTexto()).doesNotContain("127.0.0.1");
        assertThat(produto.getNome()).doesNotContain("127.0.0.1");
    }

    private static EventoProdutoRequest eventoLink(String url) {
        return new EventoProdutoRequest(TipoEvento.PRODUTO_ENVIADO, EntradaAuditoria.LINK,
                new ProdutoDoEvento(null, null, null, null, Origem.REAL, url, null, null));
    }

    private static Path ultimoSnapshot(String produtoId) throws IOException {
        try (Stream<Path> pastas = Files.list(pastaSnapshots.resolve(produtoId))) {
            return pastas.sorted().reduce((primeira, segunda) -> segunda).orElseThrow();
        }
    }

    private static EventoProdutoRequest eventoComTexto(String id, String pagina, ConteudoDoEvento conteudo) {
        return new EventoProdutoRequest(TipoEvento.PRODUTO_ENVIADO, new ProdutoDoEvento(
                id, "Método Loja Lucrativa", "curso", new BigDecimal("297.00"), Origem.SIMULADO, null, pagina, conteudo));
    }

    private static EventoProdutoRequest eventoComUrl(String id, String url, ConteudoDoEvento conteudo) {
        return new EventoProdutoRequest(TipoEvento.PRODUTO_ENVIADO, new ProdutoDoEvento(
                id, "Produto", "curso", new BigDecimal("297.00"), Origem.REAL, url, null, conteudo));
    }
}
