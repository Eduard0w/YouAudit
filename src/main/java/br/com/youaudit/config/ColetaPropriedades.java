package br.com.youaudit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.util.List;

/**
 * Configuração da coleta de páginas por URL, lida das linhas "coleta.*" do application.properties.
 * Ficam fora do código para poderem mudar sem recompilar (e para os testes usarem valores menores).
 *
 * @param userAgent          identificação enviada ao site em toda requisição
 * @param timeoutMs          tempo máximo de espera por resposta (10 000 = 10 s)
 * @param intervaloMinimoMs  intervalo mínimo entre duas requisições quaisquer (1 000 = 1 por segundo)
 * @param minimoCaracteres   abaixo disto, a auditoria fica COLETA_INSUFICIENTE
 * @param diretorioSnapshots onde gravar as cópias das páginas (dentro de data/real, fora do Git)
 * @param ipsLiberadosParaTeste IPs exatos que escapam da proteção contra SSRF. SÓ PARA TESTES (o servidor
 *                              local roda em 127.0.0.1). Vazio em produção.
 */
@ConfigurationProperties(prefix = "coleta")
public record ColetaPropriedades(
        String userAgent,
        int timeoutMs,
        long intervaloMinimoMs,
        int minimoCaracteres,
        Path diretorioSnapshots,
        List<String> ipsLiberadosParaTeste) {

    /** Sem a linha no application.properties, o Spring passa null: tratamos como lista vazia. */
    public ColetaPropriedades {
        ipsLiberadosParaTeste = ipsLiberadosParaTeste == null ? List.of() : List.copyOf(ipsLiberadosParaTeste);
    }
}
