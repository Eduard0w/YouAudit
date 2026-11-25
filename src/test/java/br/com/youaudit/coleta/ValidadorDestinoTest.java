package br.com.youaudit.coleta;

import br.com.youaudit.config.ColetaPropriedades;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Um teste para cada bloqueio da proteção contra SSRF. */
class ValidadorDestinoTest {

    private final ResolvedorDnsFalso dns = new ResolvedorDnsFalso()
            .com("publico.teste", "93.184.216.34")
            .com("localhost", "127.0.0.1", "::1")
            .com("interno.teste", "10.0.0.5")
            .com("misto.teste", "93.184.216.34", "192.168.0.10")
            .com("metadados.teste", "169.254.169.254");

    private final ValidadorDestino validador = validador(List.of());

    // ---------- esquema ----------

    @ParameterizedTest
    @ValueSource(strings = {"ftp://publico.teste/x", "file:///etc/passwd", "gopher://publico.teste/", "javascript:alert(1)"})
    void soAceitaHttpEHttps(String url) {
        assertThatThrownBy(() -> validador.validar(URI.create(url)))
                .isInstanceOf(ColetaFalhouException.class)
                .hasMessageContaining("URL inválida");
    }

    @Test
    void sitePublicoPassa() {
        assertThatCode(() -> validador.validar(URI.create("https://publico.teste/curso"))).doesNotThrowAnyException();
        assertThatCode(() -> validador.validar(URI.create("http://172.32.0.1/"))).doesNotThrowAnyException();
    }

    // ---------- loopback ----------

    @ParameterizedTest
    @ValueSource(strings = {"http://127.0.0.1/", "http://127.10.20.30:8080/", "http://localhost/", "http://[::1]/"})
    void bloqueiaLoopback(String url) {
        assertBloqueado(url);
    }

    // ---------- redes privadas ----------

    @ParameterizedTest
    @ValueSource(strings = {"http://10.0.0.1/", "http://172.16.0.1/", "http://172.31.255.255/",
            "http://192.168.1.1/", "http://[fd12:3456::1]/", "http://interno.teste/"})
    void bloqueiaRedesPrivadas(String url) {
        assertBloqueado(url);
    }

    // ---------- link-local e metadados ----------

    @ParameterizedTest
    @ValueSource(strings = {"http://169.254.10.10/", "http://[fe80::1]/"})
    void bloqueiaLinkLocal(String url) {
        assertBloqueado(url);
    }

    @Test
    void bloqueiaOServicoDeMetadadosDaNuvem() {
        assertBloqueado("http://169.254.169.254/latest/meta-data/");
        assertBloqueado("http://metadados.teste/");
    }

    // ---------- outras faixas e disfarces ----------

    @ParameterizedTest
    @ValueSource(strings = {"http://0.0.0.0/", "http://[::]/", "http://100.64.0.1/", "http://224.0.0.1/"})
    void bloqueiaQualquerEnderecoCgnatEMulticast(String url) {
        assertBloqueado(url);
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://[::ffff:10.0.0.1]/", "http://[::ffff:127.0.0.1]/", "http://[::127.0.0.1]/"})
    void bloqueiaIpv4EscondidoEmIpv6(String url) {
        assertBloqueado(url);
    }

    @Test
    void bloqueiaNomeComUmIpPublicoEOutroPrivado() {
        assertBloqueado("http://misto.teste/");
    }

    @Test
    void nomeQueNaoResolveEhRecusado() {
        assertThatThrownBy(() -> validador.validar(URI.create("http://nao-existe.teste/")))
                .isInstanceOf(ColetaFalhouException.class)
                .hasMessageContaining("DNS");
    }

    @Test
    void mensagemNaoCitaOEndereco() {
        assertThatThrownBy(() -> validador.validar(URI.create("http://interno.teste/segredo")))
                .hasMessageNotContaining("interno.teste")
                .hasMessageNotContaining("10.0.0.5");
    }

    // ---------- exceção só para testes ----------

    @Test
    void ipLiberadoParaTestePassaMasSoEleExato() {
        ValidadorDestino comExcecao = validador(List.of("127.0.0.1"));

        assertThatCode(() -> comExcecao.validar(URI.create("http://127.0.0.1:8080/"))).doesNotThrowAnyException();
        assertThatThrownBy(() -> comExcecao.validar(URI.create("http://127.0.0.2/")))
                .isInstanceOf(ColetaFalhouException.class);
    }

    @Test
    void listaDeLiberadosNaoAceitaNomes() {
        assertThatThrownBy(() -> validador(List.of("localhost"))).isInstanceOf(IllegalArgumentException.class);
    }

    private void assertBloqueado(String url) {
        assertThatThrownBy(() -> validador.validar(URI.create(url)))
                .isInstanceOf(ColetaFalhouException.class)
                .hasMessageContaining("destino bloqueado");
    }

    private ValidadorDestino validador(List<String> liberados) {
        return new ValidadorDestino(dns,
                new ColetaPropriedades("teste/0", 1000, 0, 500, Path.of("nao-usado"), liberados));
    }
}
