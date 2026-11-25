package br.com.youaudit.coleta;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Traduz um nome (ex.: "site.com") nos IPs dele. É uma interface para os testes poderem simular
 * "um nome que aponta para a rede interna" sem depender de DNS de verdade.
 */
public interface ResolvedorDns {

    List<InetAddress> resolver(String host) throws UnknownHostException;
}
