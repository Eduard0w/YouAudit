package br.com.youaudit.coleta;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/** Resolução de DNS real, pelo sistema operacional. Um IP escrito na URL ("10.0.0.1") volta direto, sem consulta. */
@Component
public class ResolvedorDnsSistema implements ResolvedorDns {

    @Override
    public List<InetAddress> resolver(String host) throws UnknownHostException {
        return List.of(InetAddress.getAllByName(host));
    }
}
