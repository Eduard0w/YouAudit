package br.com.youaudit.coleta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeitorArquivoUrlsTest {

    @TempDir
    Path pasta;

    private final LeitorArquivoUrls leitor = new LeitorArquivoUrls();

    @Test
    void leLinhasValidasEIgnoraComentariosELinhasEmBranco() throws IOException {
        LeituraUrls leitura = leitor.ler(arquivo("""
                # páginas de desenvolvimento

                pagina-01 https://exemplo.invalid/a
                pagina-02   http://exemplo.invalid/b?x=1
                """));

        assertThat(leitura.validas()).containsExactly(
                new LinhaUrl(3, "pagina-01", "https://exemplo.invalid/a"),
                new LinhaUrl(4, "pagina-02", "http://exemplo.invalid/b?x=1"));
        assertThat(leitura.linhasInvalidas()).isEmpty();
    }

    @Test
    void marcaLinhasInvalidasPeloNumero() throws IOException {
        LeituraUrls leitura = leitor.ler(arquivo("""
                pagina-01 https://exemplo.invalid/a
                https://exemplo.invalid/sem-id
                loja-do-fulano https://exemplo.invalid/c
                pagina-03 ftp://exemplo.invalid/d
                pagina-04 https://exemplo.invalid/e sobra
                pagina-01 https://exemplo.invalid/repetido
                """));

        assertThat(leitura.validas()).extracting(LinhaUrl::produtoId).containsExactly("pagina-01");
        assertThat(leitura.linhasInvalidas()).containsExactly(2, 3, 4, 5, 6);
    }

    @Test
    void arquivoAusenteLancaErro() {
        assertThatThrownBy(() -> leitor.ler(pasta.resolve("nao-existe.txt"))).isInstanceOf(NoSuchFileException.class);
    }

    private Path arquivo(String conteudo) throws IOException {
        return Files.writeString(pasta.resolve("urls.txt"), conteudo);
    }
}
