package br.com.youaudit.simulador;

import br.com.youaudit.coleta.Origem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeitorArquivoSimuladoTest {

    @Test
    void oArquivoDoProjetoTem18ProdutosTodosSimulados() {
        ArquivoSimulado a = new LeitorArquivoSimulado(new ObjectMapper(), "data/simulado/produtos.json").ler();

        assertThat(a.produtos()).hasSize(18).allSatisfy(p -> {
            assertThat(p.evento().produto().origem()).isEqualTo(Origem.SIMULADO);
            assertThat(p.evento().produto().paginaTexto()).isNotBlank();
            assertThat(p.divergenciasPlantadas()).isNotNull();
        });
        assertThat(a.reenvios()).extracting(r -> r.produto().id()).containsExactly("sim-001");
    }

    @Test
    void recusaProdutoRealNoArquivoSimulado(@TempDir Path pasta) throws IOException {
        Path arquivo = pasta.resolve("produtos.json");
        Files.writeString(arquivo, """
                {"produtos":[{"evento":{"tipo":"PRODUTO_ENVIADO","produto":{"id":"sim-001","nome":"x","preco":1,
                 "origem":"REAL","pagina_texto":"x"}},"divergencias_plantadas":[]}]}""");

        assertThatThrownBy(() -> new LeitorArquivoSimulado(new ObjectMapper(), arquivo.toString()).ler())
                .hasMessageContaining("origem SIMULADO");
    }
}
