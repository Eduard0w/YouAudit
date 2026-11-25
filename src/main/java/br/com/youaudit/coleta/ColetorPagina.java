package br.com.youaudit.coleta;

import br.com.youaudit.config.ColetaPropriedades;
import br.com.youaudit.evento.CalculadoraHash;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Passo 2 do pipeline quando o evento traz URL: baixa a página, limpa o texto e grava o snapshot.
 * Cuidados com o site: User-Agent identificável, timeout, no máximo 1 requisição por segundo e respeito ao robots.txt.
 * Cuidados com a nossa rede (SSRF): antes de CADA requisição, inclusive a cada redirecionamento, o destino passa
 * pelo {@link ValidadorDestino}; no máximo 3 redirecionamentos.
 * O log mostra só o id do produto, nunca a URL (regra 8 do CLAUDE.md).
 */
@Component
public class ColetorPagina {

    private static final Logger log = LoggerFactory.getLogger(ColetorPagina.class);

    /** Páginas maiores que 5 MB são cortadas: nenhuma página de venda razoável chega perto disso. */
    private static final int TAMANHO_MAXIMO_BYTES = 5 * 1024 * 1024;

    static final int MAXIMO_REDIRECIONAMENTOS = 3;

    private static final Set<Integer> STATUS_REDIRECIONAMENTO = Set.of(301, 302, 303, 307, 308);

    /** robots.txt quando o site falha (5xx ou rede): por cautela, tratar como "proibido tudo" (RFC 9309). */
    private static final String PROIBE_TUDO = "User-agent: *\nDisallow: /";

    private final ColetaPropriedades propriedades;
    private final LimpadorTexto limpador;
    private final CalculadoraHash calculadoraHash;
    private final LimitadorRequisicoes limitador;
    private final LeitorRobots leitorRobots;
    private final GravadorSnapshot gravador;
    private final ValidadorDestino validador;
    private final Clock relogio;

    /** robots.txt já baixado, por origem (ex.: "https://site.com"), para não baixar de novo a cada página. */
    private final Map<String, String> robotsPorOrigem = new ConcurrentHashMap<>();

    public ColetorPagina(ColetaPropriedades propriedades, LimpadorTexto limpador, CalculadoraHash calculadoraHash,
                         LimitadorRequisicoes limitador, LeitorRobots leitorRobots, GravadorSnapshot gravador,
                         ValidadorDestino validador, Clock relogio) {
        this.propriedades = propriedades;
        this.limpador = limpador;
        this.calculadoraHash = calculadoraHash;
        this.limitador = limitador;
        this.leitorRobots = leitorRobots;
        this.gravador = gravador;
        this.validador = validador;
        this.relogio = relogio;
    }

    public ResultadoColeta coletar(String produtoId, String url) {
        String html = baixarSeguindoRedirecionamentos(converter(url));
        String texto = limpador.limpar(html);
        String hash = calculadoraHash.sha256(texto);
        Path pasta = gravador.gravar(produtoId, url, html, texto, hash, Instant.now(relogio));
        log.info("Coleta do produto {}: {} caracteres de texto limpo", produtoId, texto.length());
        return new ResultadoColeta(texto, hash, pasta);
    }

    /**
     * Segue os redirecionamentos à mão (o jsoup não segue sozinho), para validar cada destino ANTES de conectar.
     * Sem isso, um site público poderia redirecionar para http://169.254.169.254/ e o servidor iria atrás.
     */
    private String baixarSeguindoRedirecionamentos(URI inicial) {
        URI atual = inicial;
        for (int redirecionamentos = 0; ; redirecionamentos++) {
            validador.validar(atual);
            if (!robotsPermite(atual)) {
                throw new ColetaFalhouException("o robots.txt do site não permite coletar esta página");
            }
            Connection.Response resposta = requisitar(atual);
            int status = resposta.statusCode();
            if (STATUS_REDIRECIONAMENTO.contains(status)) {
                if (redirecionamentos >= MAXIMO_REDIRECIONAMENTOS) {
                    throw new ColetaFalhouException("mais de " + MAXIMO_REDIRECIONAMENTOS + " redirecionamentos");
                }
                atual = proximoDestino(atual, resposta.header("Location"));
                continue;
            }
            if (status < 200 || status >= 300) {
                throw new ColetaFalhouException("o site respondeu HTTP " + status);
            }
            return resposta.body();
        }
    }

    private static URI proximoDestino(URI atual, String location) {
        if (location == null || location.isBlank()) {
            throw new ColetaFalhouException("redirecionamento sem destino");
        }
        try {
            // resolve() aceita destino relativo ("/outra") ou absoluto ("https://outro.site/").
            return atual.resolve(location.strip());
        } catch (IllegalArgumentException e) {
            throw new ColetaFalhouException("redirecionamento para um endereço inválido");
        }
    }

    private static URI converter(String url) {
        try {
            return URI.create(url.strip());
        } catch (IllegalArgumentException e) {
            throw new ColetaFalhouException("URL inválida: use um endereço http:// ou https://");
        }
    }

    private boolean robotsPermite(URI endereco) {
        String origem = endereco.getScheme() + "://" + endereco.getRawAuthority();
        String robots = robotsPorOrigem.computeIfAbsent(origem, this::baixarRobots);
        String caminho = endereco.getRawPath() == null || endereco.getRawPath().isEmpty() ? "/" : endereco.getRawPath();
        if (endereco.getRawQuery() != null) {
            caminho += "?" + endereco.getRawQuery();
        }
        return leitorRobots.permite(robots, nomeDoRobo(), caminho);
    }

    /**
     * 2xx: usa o arquivo. 3xx ou 4xx: sem regras (não seguimos redirecionamento do robots.txt, para não abrir
     * outra porta de SSRF). 5xx ou falha de rede: proíbe tudo, por cautela.
     * O host já foi validado: o robots.txt fica no mesmo site da página.
     */
    private String baixarRobots(String origem) {
        limitador.aguardarVez();
        try {
            Connection.Response resposta = conexao(origem + "/robots.txt").ignoreContentType(true).execute();
            int status = resposta.statusCode();
            if (status >= 200 && status < 300) {
                return resposta.body();
            }
            return status >= 300 && status < 500 ? "" : PROIBE_TUDO;
        } catch (IOException e) {
            return PROIBE_TUDO;
        }
    }

    private Connection.Response requisitar(URI endereco) {
        limitador.aguardarVez();
        try {
            return conexao(endereco.toString()).execute();
        } catch (SocketTimeoutException e) {
            throw new ColetaFalhouException("tempo esgotado ao baixar a página (" + propriedades.timeoutMs() / 1000 + " s)");
        } catch (UnsupportedMimeTypeException e) {
            throw new ColetaFalhouException("o endereço não devolveu uma página HTML");
        } catch (IOException e) {
            // Só o tipo do erro: a mensagem original do Java costuma conter a URL.
            throw new ColetaFalhouException("falha de rede ao baixar a página (" + e.getClass().getSimpleName() + ")");
        }
    }

    /** followRedirects(false): os redirecionamentos são tratados acima. ignoreHttpErrors: o status é conferido acima. */
    private Connection conexao(String url) {
        return Jsoup.connect(url)
                .userAgent(propriedades.userAgent())
                .timeout(propriedades.timeoutMs())
                .maxBodySize(TAMANHO_MAXIMO_BYTES)
                .followRedirects(false)
                .ignoreHttpErrors(true);
    }

    /** Nome curto do robô para o robots.txt: o que vem antes da "/" no User-Agent ("YouAudit-prototipo"). */
    private String nomeDoRobo() {
        String ua = propriedades.userAgent();
        int barra = ua.indexOf('/');
        return barra > 0 ? ua.substring(0, barra) : ua;
    }
}
