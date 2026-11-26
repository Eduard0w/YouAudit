package br.com.youaudit.avaliacao;

import br.com.youaudit.extracao.TipoAfirmacao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Usa arquivos temporários fictícios: o gabarito real fica fora do Git e o teste não pode depender dele. */
class LeitorGabaritoTest {

    @TempDir Path pasta;

    @Test
    void leCampoComAspasQuebraDeLinhaEAspasDuplicadas() throws IOException {
        List<RotuloAfirmacao> r = ler("id,origem,pagina_id,tipo,trecho,divisao\r\n"
                + "a001,REAL,pagina-01,GARANTIA,\"Garantia:\r\nVocê tem 7 dias, \"\"sem burocracia\"\".\",DESENVOLVIMENTO\r\n"
                + "a002,REAL,pagina-02,ACESSO,Acesso vitalício,RESERVADO");

        assertThat(r).hasSize(2);
        assertThat(r.get(0).trecho()).isEqualTo("Garantia:\nVocê tem 7 dias, \"sem burocracia\".");
        assertThat(r.get(0).tipo()).isEqualTo(TipoAfirmacao.GARANTIA);
        assertThat(r.get(1).divisao()).isEqualTo("RESERVADO");
    }

    @Test
    void cabecalhoErradoDerrubaALeitura() {
        assertThatThrownBy(() -> ler("id,tipo\na1,OUTRO")).hasMessageContaining("cabeçalho");
    }

    @Test
    void tipoInvalidoDizQualRegistro() {
        assertThatThrownBy(() -> ler("id,origem,pagina_id,tipo,trecho,divisao\na9,REAL,pagina-01,PROMESSA,x,DESENVOLVIMENTO"))
                .hasMessageContaining("a9");
    }

    private List<RotuloAfirmacao> ler(String conteudo) throws IOException {
        Path arquivo = pasta.resolve("afirmacoes.csv");
        Files.writeString(arquivo, conteudo);
        return new LeitorGabarito(arquivo.toString()).ler();
    }
}
