package br.com.youaudit.evento;

import br.com.youaudit.coleta.LeitorArquivoUrls;
import br.com.youaudit.coleta.LeituraUrls;
import br.com.youaudit.coleta.LinhaUrl;
import br.com.youaudit.coleta.Origem;
import br.com.youaudit.config.LotePropriedades;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lê data/real/urls.txt e dispara um evento PRODUTO_ENVIADO por link, com origem REAL e entrada EVENTO
 * (o lote faz o papel do gatilho simulado). Um evento por segundo (lote.intervalo-ms), além do limite de
 * 1 requisição por segundo do coletor.
 *
 * <p>Responde na hora e processa em segundo plano, no pool de threads: com 10 links, esperar tudo
 * levaria mais de 10 s. Só um lote por vez. O log mostra o id ("pagina-03"), nunca a URL.
 */
@Service
public class ServicoLote {

    private static final Logger log = LoggerFactory.getLogger(ServicoLote.class);

    private final LeitorArquivoUrls leitor;
    private final ServicoEvento servicoEvento;
    private final LotePropriedades propriedades;
    private final TaskExecutor executor;
    private final AtomicBoolean rodando = new AtomicBoolean(false);

    public ServicoLote(LeitorArquivoUrls leitor, ServicoEvento servicoEvento, LotePropriedades propriedades,
                       TaskExecutor executor) {
        this.leitor = leitor;
        this.servicoEvento = servicoEvento;
        this.propriedades = propriedades;
        this.executor = executor;
    }

    public RespostaLote iniciar() {
        // compareAndSet: troca false→true só se ninguém trocou antes. É a "trava" contra dois lotes juntos.
        if (!rodando.compareAndSet(false, true)) {
            throw new LoteEmAndamentoException();
        }
        LeituraUrls leitura;
        try {
            leitura = leitor.ler(propriedades.arquivoUrls());
        } catch (IOException e) {
            rodando.set(false);
            throw new ArquivoUrlsIndisponivelException();
        }
        List<LinhaUrl> linhas = leitura.validas();
        try {
            executor.execute(() -> processar(linhas));
        } catch (RuntimeException e) {
            rodando.set(false);
            throw e;
        }
        log.info("Lote iniciado: {} links, {} linhas inválidas", linhas.size(), leitura.linhasInvalidas().size());
        return new RespostaLote(linhas.size(), leitura.linhasInvalidas(), propriedades.intervaloMs());
    }

    public boolean emAndamento() {
        return rodando.get();
    }

    private void processar(List<LinhaUrl> linhas) {
        try {
            for (int i = 0; i < linhas.size(); i++) {
                if (i > 0 && !esperar()) {
                    return;
                }
                disparar(linhas.get(i));
            }
        } finally {
            rodando.set(false);
        }
    }

    /** Um link com problema não interrompe os outros: registra e segue. */
    private void disparar(LinhaUrl linha) {
        ProdutoDoEvento produto = new ProdutoDoEvento(linha.produtoId(), linha.produtoId(), null, null, Origem.REAL,
                linha.url(), null, null);
        try {
            EventoResposta resposta = servicoEvento.receber(
                    new EventoProdutoRequest(TipoEvento.PRODUTO_ENVIADO, EntradaAuditoria.EVENTO, produto));
            log.info("Lote: {} → {} {}", linha.produtoId(), resposta.resultado(), resposta.statusAuditoria());
        } catch (RuntimeException e) {
            log.warn("Lote: {} não processado ({})", linha.produtoId(), e.getClass().getSimpleName());
        }
    }

    private boolean esperar() {
        try {
            Thread.sleep(propriedades.intervaloMs());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
