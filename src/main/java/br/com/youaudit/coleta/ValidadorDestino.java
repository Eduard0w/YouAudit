package br.com.youaudit.coleta;

import br.com.youaudit.config.ColetaPropriedades;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;

/**
 * Proteção contra SSRF ("Server-Side Request Forgery"): impede que alguém use o formulário "Auditar um link"
 * para fazer o servidor acessar a rede interna (o próprio computador, o roteador, o serviço de metadados da nuvem).
 *
 * <p>Regras: só http/https; o nome é resolvido no DNS e, se QUALQUER IP dele cair numa faixa bloqueada, a coleta
 * é recusada. Faixas bloqueadas: loopback (127/8, ::1), "qualquer endereço" (0.0.0.0, ::), redes privadas
 * (10/8, 172.16/12, 192.168/16, fc00::/7), link-local (169.254/16, fe80::/10, incluindo 169.254.169.254 dos
 * metadados de nuvem), CGNAT (100.64/10) e multicast.
 *
 * <p>Limite conhecido (DNS rebinding): o jsoup resolve o nome de novo ao conectar, e não dá para fixar o IP
 * validado. Um domínio malicioso pode trocar de IP entre a validação e a conexão. Fechar isso exige outro
 * cliente HTTP; no protótipo, que roda local, o limite fica declarado.
 */
@Component
public class ValidadorDestino {

    private final ResolvedorDns resolvedor;
    private final List<InetAddress> ipsLiberados;

    public ValidadorDestino(ResolvedorDns resolvedor, ColetaPropriedades propriedades) {
        this.resolvedor = resolvedor;
        this.ipsLiberados = propriedades.ipsLiberadosParaTeste().stream().map(ValidadorDestino::ipLiteral).toList();
    }

    /** Lança {@link ColetaFalhouException} se o destino não for permitido. As mensagens nunca citam o endereço. */
    public void validar(URI endereco) {
        String esquema = endereco.getScheme() == null ? "" : endereco.getScheme().toLowerCase(Locale.ROOT);
        if (!esquema.equals("http") && !esquema.equals("https")) {
            throw new ColetaFalhouException("URL inválida: use um endereço http:// ou https://");
        }
        String host = endereco.getHost();
        if (host == null || host.isBlank()) {
            throw new ColetaFalhouException("URL inválida: endereço sem nome de site");
        }
        // Em URLs com IPv6 o host vem entre colchetes: "[::1]".
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }

        List<InetAddress> ips;
        try {
            ips = resolvedor.resolver(host);
        } catch (UnknownHostException e) {
            throw new ColetaFalhouException("não foi possível resolver o nome do site no DNS");
        }
        if (ips.isEmpty()) {
            throw new ColetaFalhouException("não foi possível resolver o nome do site no DNS");
        }
        for (InetAddress ip : ips) {
            if (!ipsLiberados.contains(ip) && bloqueado(ip)) {
                throw new ColetaFalhouException("destino bloqueado: o endereço aponta para a rede interna "
                        + "(loopback, rede privada ou link-local)");
            }
        }
    }

    static boolean bloqueado(InetAddress ip) {
        if (ip.isAnyLocalAddress() || ip.isLoopbackAddress() || ip.isLinkLocalAddress()
                || ip.isSiteLocalAddress() || ip.isMulticastAddress()) {
            return true;
        }
        byte[] b = ip.getAddress();
        if (ip instanceof Inet4Address) {
            int primeiro = b[0] & 0xFF;
            int segundo = b[1] & 0xFF;
            return primeiro == 0                                        // 0.0.0.0/8
                    || (primeiro == 100 && segundo >= 64 && segundo <= 127) // 100.64.0.0/10 (CGNAT)
                    || (primeiro == 169 && segundo == 254);             // 169.254.0.0/16, inclui 169.254.169.254
        }
        if (ip instanceof Inet6Address ip6) {
            if ((b[0] & 0xFE) == 0xFC) {                                // fc00::/7 (rede privada IPv6)
                return true;
            }
            if (ip6.isIPv4CompatibleAddress()) {                        // ::a.b.c.d esconde um IPv4
                try {
                    return bloqueado(InetAddress.getByAddress(new byte[]{b[12], b[13], b[14], b[15]}));
                } catch (UnknownHostException e) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Converte um IP escrito na configuração. Só aceita IP literal: um nome aqui seria um buraco na proteção. */
    private static InetAddress ipLiteral(String texto) {
        String limpo = texto.strip();
        if (!limpo.matches("[0-9a-fA-F:.]+")) {
            throw new IllegalArgumentException("coleta.ips-liberados-para-teste aceita só IPs, não nomes: " + limpo);
        }
        try {
            return InetAddress.getByName(limpo);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("IP inválido em coleta.ips-liberados-para-teste: " + limpo, e);
        }
    }
}
