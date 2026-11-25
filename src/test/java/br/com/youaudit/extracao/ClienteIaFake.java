package br.com.youaudit.extracao;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * IA falsa para os testes: devolve, na ordem, as respostas (ou falhas) programadas e anota cada pedido recebido.
 * Sem resposta programada, falha alto, para o teste não passar por acaso.
 */
public final class ClienteIaFake implements ClienteIa {

    private final Deque<Object> respostas = new ArrayDeque<>();
    private final List<PedidoIa> pedidos = new ArrayList<>();

    public ClienteIaFake responder(String json) {
        respostas.add(json);
        return this;
    }

    public ClienteIaFake falhar(String motivo) {
        respostas.add(new FalhaIaException(motivo));
        return this;
    }

    public List<PedidoIa> pedidos() {
        return pedidos;
    }

    @Override
    public String modelo() {
        return "fake";
    }

    @Override
    public String gerarJson(PedidoIa pedido) {
        pedidos.add(pedido);
        Object proxima = respostas.poll();
        if (proxima == null) {
            throw new IllegalStateException("ClienteIaFake: nenhuma resposta programada para a chamada " + pedidos.size());
        }
        if (proxima instanceof FalhaIaException falha) {
            throw falha;
        }
        return (String) proxima;
    }
}
