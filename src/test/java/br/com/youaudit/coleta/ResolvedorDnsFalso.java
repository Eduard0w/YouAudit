package br.com.youaudit.coleta;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DNS de mentira para os testes: nomes cadastrados resolvem para os IPs escolhidos; IP escrito na URL volta direto;
 * qualquer outro nome dá "não encontrado". Nenhuma consulta de rede acontece.
 */
public final class ResolvedorDnsFalso implements ResolvedorDns {

    private final Map<String, List<InetAddress>> nomes = new HashMap<>();

    public ResolvedorDnsFalso com(String nome, String... ips) {
        nomes.put(nome, Arrays.stream(ips).map(ResolvedorDnsFalso::literal).toList());
        return this;
    }

    @Override
    public List<InetAddress> resolver(String host) throws UnknownHostException {
        if (nomes.containsKey(host)) {
            return nomes.get(host);
        }
        if (host.matches("[0-9.]+") || host.contains(":")) {
            return List.of(InetAddress.getByName(host)); // IP literal: o Java converte sem consultar DNS
        }
        throw new UnknownHostException("nome não cadastrado no DNS falso");
    }

    private static InetAddress literal(String ip) {
        try {
            return InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
