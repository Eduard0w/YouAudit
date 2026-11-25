package br.com.youaudit.coleta;

import br.com.youaudit.config.ColetaPropriedades;
import br.com.youaudit.evento.CalculadoraHash;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;

import static br.com.youaudit.coleta.ServidorHttpLocal.recurso;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Coleta de verdade, por HTTP, contra um servidor local com uma página HTML fictícia. */
class ColetorPaginaTest {

    private static final String USER_AGENT = "YouAudit-prototipo/0.1 (teste)";

    @TempDir
    Path pastaSnapshots;

    private ServidorHttpLocal servidor;
    private ColetorPagina coletor;
    private final CalculadoraHash hash = new CalculadoraHash();

    @BeforeEach
    void preparar() {
        servidor = ServidorHttpLocal.iniciar()
                .robots("User-agent: *\nDisallow: /privado")
                .html("/curso", recurso("/paginas/pagina-longa.html"))
                .html("/privado/curso", recurso("/paginas/pagina-longa.html"));
        // Intervalo 0 nos testes para não esperar; o limitador tem teste próprio.
        // 127.0.0.1 liberado só aqui, porque o servidor de teste roda nele. Todo o resto segue bloqueado.
        ColetaPropriedades propriedades =
                new ColetaPropriedades(USER_AGENT, 2000, 0, 500, pastaSnapshots, List.of("127.0.0.1"));
        ResolvedorDnsFalso dns = new ResolvedorDnsFalso().com("interno.teste", "10.0.0.5");
        coletor = new ColetorPagina(propriedades, new LimpadorTexto(), hash, new LimitadorRequisicoes(propriedades),
                new LeitorRobots(), new GravadorSnapshot(propriedades), new ValidadorDestino(dns, propriedades),
                Clock.systemUTC());
    }

    @AfterEach
    void desligar() {
        servidor.close();
    }

    @Test
    void coletaPaginaLocalEGravaSnapshotComOsTresArquivos() throws IOException {
        ResultadoColeta resultado = coletor.coletar("pagina-teste", servidor.url("/curso"));

        Path pasta = resultado.pastaSnapshot();
        assertThat(pasta).startsWith(pastaSnapshots.resolve("pagina-teste"));
        assertThat(Files.readString(pasta.resolve("pagina.html"))).isEqualTo(recurso("/paginas/pagina-longa.html"));
        assertThat(Files.readString(pasta.resolve("texto.txt"))).isEqualTo(resultado.textoLimpo());
        assertThat(Files.readString(pasta.resolve("meta.json")))
                .contains("\"produto_id\" : \"pagina-teste\"", "\"hash_texto\" : \"" + resultado.hash() + "\"");
    }

    @Test
    void hashEhODoTextoGravadoNoSnapshot() throws IOException {
        ResultadoColeta resultado = coletor.coletar("pagina-teste", servidor.url("/curso"));

        String textoDoSnapshot = Files.readString(resultado.pastaSnapshot().resolve("texto.txt"));
        assertThat(resultado.hash()).isEqualTo(hash.sha256(textoDoSnapshot));
    }

    @Test
    void textoLimpoNaoTemHtmlNemScriptEMantemOsParagrafos() {
        String texto = coletor.coletar("pagina-teste", servidor.url("/curso")).textoLimpo();

        assertThat(texto).doesNotContain("<", "console.log", "font-family");
        assertThat(texto).contains("Fature R$10 mil por mês em 30 dias\n", "Garantia de 7 dias");
        assertThat(texto.length()).isGreaterThanOrEqualTo(500);
    }

    @Test
    void enviaOUserAgentConfigurado() {
        coletor.coletar("pagina-teste", servidor.url("/curso"));

        assertThat(servidor.ultimoUserAgent()).isEqualTo(USER_AGENT);
    }

    @Test
    void caminhoProibidoNoRobotsNaoEhBaixado() {
        assertThatThrownBy(() -> coletor.coletar("pagina-teste", servidor.url("/privado/curso")))
                .isInstanceOf(ColetaFalhouException.class)
                .hasMessageContaining("robots.txt");
        assertThat(servidor.pedidos()).containsExactly("/robots.txt");
    }

    @Test
    void robotsBaixadoUmaVezPorSite() {
        coletor.coletar("pagina-teste", servidor.url("/curso"));
        coletor.coletar("pagina-teste", servidor.url("/curso"));

        assertThat(servidor.pedidos()).containsExactly("/robots.txt", "/curso", "/curso");
    }

    @Test
    void siteSemRobotsPermiteAColeta() {
        try (ServidorHttpLocal semRobots = ServidorHttpLocal.iniciar()
                .html("/curso", recurso("/paginas/pagina-longa.html"))) {
            assertThat(coletor.coletar("pagina-teste", semRobots.url("/curso")).textoLimpo()).isNotEmpty();
        }
    }

    @Test
    void erroHttpViraFalhaSemAUrlNaMensagem() {
        assertThatThrownBy(() -> coletor.coletar("pagina-teste", servidor.url("/nao-existe")))
                .isInstanceOf(ColetaFalhouException.class)
                .hasMessageContaining("HTTP 404")
                .hasMessageNotContaining("127.0.0.1")
                .hasMessageNotContaining("nao-existe");
    }

    // ---------- redirecionamentos (SSRF) ----------

    @Test
    void segueAteTresRedirecionamentos() {
        servidor.redirecionar("/r1", "/r2").redirecionar("/r2", "/r3").redirecionar("/r3", "/curso");

        assertThat(coletor.coletar("pagina-teste", servidor.url("/r1")).textoLimpo()).contains("Método Loja Lucrativa");
        assertThat(servidor.pedidos()).containsExactly("/robots.txt", "/r1", "/r2", "/r3", "/curso");
    }

    @Test
    void quartoRedirecionamentoEhRecusado() {
        servidor.redirecionar("/r1", "/r2").redirecionar("/r2", "/r3").redirecionar("/r3", "/r4")
                .redirecionar("/r4", "/curso");

        assertThatThrownBy(() -> coletor.coletar("pagina-teste", servidor.url("/r1")))
                .isInstanceOf(ColetaFalhouException.class)
                .hasMessageContaining("mais de 3 redirecionamentos");
        assertThat(servidor.pedidos()).doesNotContain("/curso");
    }

    @Test
    void redirecionamentoParaMetadadosDaNuvemEhBloqueadoAntesDeConectar() {
        servidor.redirecionar("/sai", "http://169.254.169.254/latest/meta-data/");

        // A mensagem é "destino bloqueado" (validação), e não "tempo esgotado" (tentativa de conexão).
        assertThatThrownBy(() -> coletor.coletar("pagina-teste", servidor.url("/sai")))
                .isInstanceOf(ColetaFalhouException.class)
                .hasMessageContaining("destino bloqueado");
    }

    @Test
    void redirecionamentoParaNomeQueResolveParaRedePrivadaEhBloqueado() {
        servidor.redirecionar("/sai", "http://interno.teste/admin");

        assertThatThrownBy(() -> coletor.coletar("pagina-teste", servidor.url("/sai")))
                .isInstanceOf(ColetaFalhouException.class)
                .hasMessageContaining("destino bloqueado");
    }

    @Test
    void redirecionamentoParaOutraPortaLocalNaoLiberadaPorIpTambemPassaPeloValidador() {
        // localhost não está no DNS falso: sem resolver, sem conexão.
        servidor.redirecionar("/sai", "http://localhost:1/");

        assertThatThrownBy(() -> coletor.coletar("pagina-teste", servidor.url("/sai")))
                .isInstanceOf(ColetaFalhouException.class)
                .hasMessageContaining("DNS");
    }

    @Test
    void urlQueNaoEhHttpEhRecusada() {
        assertThatThrownBy(() -> coletor.coletar("pagina-teste", "file:///etc/passwd"))
                .isInstanceOf(ColetaFalhouException.class)
                .hasMessageContaining("URL inválida");
    }

    @Test
    void idDeProdutoComBarrasNaoGravaForaDoDiretorio() {
        ResultadoColeta resultado = coletor.coletar("../fora", servidor.url("/curso"));

        assertThat(resultado.pastaSnapshot().normalize()).startsWith(pastaSnapshots);
    }
}
