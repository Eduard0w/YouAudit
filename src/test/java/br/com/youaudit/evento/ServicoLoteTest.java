package br.com.youaudit.evento;

import br.com.youaudit.coleta.LeitorArquivoUrls;
import br.com.youaudit.coleta.Origem;
import br.com.youaudit.config.LotePropriedades;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** O ServicoEvento é um dublê: aqui só interessa o que o lote dispara, em que ordem e com que intervalo. */
class ServicoLoteTest {

    @TempDir
    Path pasta;

    private final ServicoEvento servicoEvento = mock(ServicoEvento.class);

    @Test
    void disparaUmEventoPorLinkComOrigemRealEEntradaEvento() throws IOException {
        when(servicoEvento.receber(any())).thenReturn(resposta());
        ServicoLote lote = lote(urls("pagina-01 https://exemplo.invalid/a\npagina-02 https://exemplo.invalid/b\n"),
                0, Runnable::run);

        RespostaLote resposta = lote.iniciar();

        assertThat(resposta.eventosAgendados()).isEqualTo(2);
        ArgumentCaptor<EventoProdutoRequest> pedidos = ArgumentCaptor.forClass(EventoProdutoRequest.class);
        verify(servicoEvento, times(2)).receber(pedidos.capture());
        assertThat(pedidos.getAllValues()).allSatisfy(p -> {
            assertThat(p.tipo()).isEqualTo(TipoEvento.PRODUTO_ENVIADO);
            assertThat(p.entrada()).isEqualTo(EntradaAuditoria.EVENTO);
            assertThat(p.produto().origem()).isEqualTo(Origem.REAL);
        });
        assertThat(pedidos.getAllValues()).extracting(p -> p.produto().id()).containsExactly("pagina-01", "pagina-02");
        assertThat(pedidos.getAllValues().get(0).produto().nome()).isEqualTo("pagina-01");
    }

    @Test
    void esperaOIntervaloEntreEventos() throws IOException {
        when(servicoEvento.receber(any())).thenReturn(resposta());
        ServicoLote lote = lote(urls("pagina-01 https://a.invalid/\npagina-02 https://b.invalid/\npagina-03 https://c.invalid/\n"),
                300, Runnable::run);

        long inicio = System.nanoTime();
        lote.iniciar();
        long ms = (System.nanoTime() - inicio) / 1_000_000L;

        // 3 eventos = 2 intervalos de 300 ms.
        assertThat(ms).isGreaterThanOrEqualTo(580);
    }

    @Test
    void informaLinhasInvalidasSemConteudo() throws IOException {
        when(servicoEvento.receber(any())).thenReturn(resposta());
        ServicoLote lote = lote(urls("pagina-01 https://a.invalid/\nlinha ruim\n"), 0, Runnable::run);

        assertThat(lote.iniciar().linhasInvalidas()).containsExactly(2);
    }

    @Test
    void umLinkComErroNaoInterrompeOsOutros() throws IOException {
        when(servicoEvento.receber(any())).thenThrow(new RuntimeException("falha")).thenReturn(resposta());
        ServicoLote lote = lote(urls("pagina-01 https://a.invalid/\npagina-02 https://b.invalid/\n"), 0, Runnable::run);

        lote.iniciar();

        verify(servicoEvento, times(2)).receber(any());
        assertThat(lote.emAndamento()).isFalse();
    }

    @Test
    void segundoLoteEnquantoOPrimeiroRodaDa409() throws IOException {
        List<Runnable> naFila = new ArrayList<>();
        ServicoLote lote = lote(urls("pagina-01 https://a.invalid/\n"), 0, naFila::add); // não executa: fica "rodando"

        lote.iniciar();

        assertThatThrownBy(lote::iniciar).isInstanceOf(LoteEmAndamentoException.class);
        naFila.forEach(Runnable::run);
        assertThat(lote.emAndamento()).isFalse();
    }

    @Test
    void arquivoAusenteDa404ELiberaATrava() {
        ServicoLote lote = lote(pasta.resolve("nao-existe.txt"), 0, Runnable::run);

        assertThatThrownBy(lote::iniciar).isInstanceOf(ArquivoUrlsIndisponivelException.class);
        assertThat(lote.emAndamento()).isFalse();
    }

    private ServicoLote lote(Path arquivo, long intervaloMs, org.springframework.core.task.TaskExecutor executor) {
        return new ServicoLote(new LeitorArquivoUrls(), servicoEvento, new LotePropriedades(arquivo, intervaloMs), executor);
    }

    private Path urls(String conteudo) throws IOException {
        return Files.writeString(pasta.resolve("urls.txt"), conteudo);
    }

    private static EventoResposta resposta() {
        return new EventoResposta(1L, "pagina-01", ResultadoEvento.PROCESSADO, 1L, 1, StatusAuditoria.EM_ANALISE,
                EntradaAuditoria.EVENTO);
    }
}
