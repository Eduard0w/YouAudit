package br.com.youaudit.simulador;

import br.com.youaudit.coleta.Origem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Lê {@code data/simulado/produtos.json}. Recusa o arquivo inteiro se algum produto não for SIMULADO (regra 7: nunca
 * misturar dado real com fictício) ou não tiver id "sim-".
 */
@Component
public class LeitorArquivoSimulado {

    private final ObjectMapper mapper;
    private final Path arquivo;

    public LeitorArquivoSimulado(ObjectMapper mapper, @Value("${simulador.arquivo}") String arquivo) {
        this.mapper = mapper;
        this.arquivo = Path.of(arquivo);
    }

    public ArquivoSimulado ler() {
        if (!Files.exists(arquivo)) {
            throw new IllegalStateException("arquivo de produtos simulados não encontrado: " + arquivo);
        }
        ArquivoSimulado lido;
        try {
            lido = mapper.readValue(arquivo.toFile(), ArquivoSimulado.class);
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui ler " + arquivo, e);
        }
        lido.produtos().forEach(p -> conferir(p.evento().produto().id(), p.evento().produto().origem()));
        lido.reenvios().forEach(r -> conferir(r.produto().id(), r.produto().origem()));
        return lido;
    }

    private static void conferir(String id, Origem origem) {
        if (origem != Origem.SIMULADO || id == null || !id.startsWith("sim-")) {
            throw new IllegalStateException("produto " + id + " no arquivo simulado precisa ter origem SIMULADO e id sim-NNN");
        }
    }
}
