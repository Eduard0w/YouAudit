package br.com.youaudit.coleta;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Servidor HTTP de teste, do próprio JDK (sem dependência nova). Roda em 127.0.0.1 numa porta livre,
 * serve as respostas cadastradas e anota cada caminho pedido e o User-Agent recebido.
 */
public final class ServidorHttpLocal implements AutoCloseable {

    private record Resposta(int status, String tipo, String corpo, String location) {
    }

    private final HttpServer servidor;
    private final Map<String, Resposta> rotas = new ConcurrentHashMap<>();
    private final List<String> pedidos = new CopyOnWriteArrayList<>();
    private volatile String ultimoUserAgent;

    private ServidorHttpLocal(HttpServer servidor) {
        this.servidor = servidor;
        servidor.createContext("/", this::atender);
        servidor.start();
    }

    public static ServidorHttpLocal iniciar() {
        try {
            // Porta 0 = o sistema escolhe uma livre.
            return new ServidorHttpLocal(HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public ServidorHttpLocal responder(String caminho, int status, String tipo, String corpo) {
        rotas.put(caminho, new Resposta(status, tipo, corpo, null));
        return this;
    }

    /** Responde 302 mandando o cliente para {@code destino} (relativo ou absoluto). */
    public ServidorHttpLocal redirecionar(String caminho, String destino) {
        rotas.put(caminho, new Resposta(302, "text/plain", "", destino));
        return this;
    }

    public ServidorHttpLocal html(String caminho, String corpo) {
        return responder(caminho, 200, "text/html; charset=utf-8", corpo);
    }

    public ServidorHttpLocal robots(String corpo) {
        return responder("/robots.txt", 200, "text/plain; charset=utf-8", corpo);
    }

    public String url(String caminho) {
        return "http://127.0.0.1:" + servidor.getAddress().getPort() + caminho;
    }

    public List<String> pedidos() {
        return pedidos;
    }

    public String ultimoUserAgent() {
        return ultimoUserAgent;
    }

    /** Lê um arquivo de src/test/resources (ex.: "/paginas/pagina-longa.html"). */
    public static String recurso(String caminho) {
        try (InputStream entrada = ServidorHttpLocal.class.getResourceAsStream(caminho)) {
            if (entrada == null) {
                throw new IllegalArgumentException("recurso não encontrado: " + caminho);
            }
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void atender(HttpExchange troca) throws IOException {
        String caminho = troca.getRequestURI().getRawPath();
        pedidos.add(caminho);
        ultimoUserAgent = troca.getRequestHeaders().getFirst("User-Agent");
        Resposta resposta = rotas.getOrDefault(caminho, new Resposta(404, "text/plain", "não encontrado", null));
        byte[] corpo = resposta.corpo().getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().set("Content-Type", resposta.tipo());
        if (resposta.location() != null) {
            troca.getResponseHeaders().set("Location", resposta.location());
        }
        troca.sendResponseHeaders(resposta.status(), corpo.length == 0 ? -1 : corpo.length);
        try (OutputStream saida = troca.getResponseBody()) {
            saida.write(corpo);
        }
    }

    @Override
    public void close() {
        servidor.stop(0);
    }
}
