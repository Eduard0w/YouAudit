package br.com.youaudit.simulador;

import br.com.youaudit.evento.EventoProdutoRequest;
import br.com.youaudit.evento.EventoResposta;
import br.com.youaudit.evento.LoteEmAndamentoException;
import br.com.youaudit.evento.ServicoEvento;
import br.com.youaudit.revisao.NaoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * O GATILHO SIMULADO da demo (regra 10: isso é declarado na tela, no README e no pitch): dispara os produtos
 * fictícios de {@code data/simulado/produtos.json} como eventos "produto enviado", um a cada
 * {@code simulador.intervalo-ms}, em segundo plano. A fila vai se enchendo sozinha enquanto o pipeline roda.
 *
 * <p>Também faz o REENVIO de um produto corrigido (para a demo mostrar o score caindo na versão 2).
 */
@Service
public class SimuladorEventos {

    private static final Logger log = LoggerFactory.getLogger(SimuladorEventos.class);

    private final LeitorArquivoSimulado leitor;
    private final ServicoEvento servicoEvento;
    private final TaskExecutor executor;
    private final long intervaloMs;
    private final AtomicBoolean rodando = new AtomicBoolean(false);

    public SimuladorEventos(LeitorArquivoSimulado leitor, ServicoEvento servicoEvento, TaskExecutor executor,
                            @Value("${simulador.intervalo-ms}") long intervaloMs) {
        this.leitor = leitor;
        this.servicoEvento = servicoEvento;
        this.executor = executor;
        this.intervaloMs = intervaloMs;
    }

    /** @return quantos eventos foram agendados */
    public int iniciar() {
        if (!rodando.compareAndSet(false, true)) {
            throw new LoteEmAndamentoException();
        }
        List<EventoProdutoRequest> eventos;
        try {
            eventos = leitor.ler().produtos().stream().map(ArquivoSimulado.ProdutoSimulado::evento).toList();
            executor.execute(() -> disparar(eventos));
        } catch (RuntimeException e) {
            rodando.set(false);
            throw e;
        }
        return eventos.size();
    }

    /** Envia a versão corrigida do produto (lista "reenvios" do arquivo). Síncrono: é um evento só. */
    public EventoResposta reenviar(String produtoId) {
        EventoProdutoRequest reenvio = leitor.ler().reenvios().stream()
                .filter(r -> r.produto().id().equals(produtoId)).findFirst()
                .orElseThrow(() -> new NaoEncontradoException("não há versão corrigida de " + produtoId + " no arquivo"));
        return servicoEvento.receber(reenvio);
    }

    private void disparar(List<EventoProdutoRequest> eventos) {
        try {
            for (int i = 0; i < eventos.size(); i++) {
                if (i > 0) {
                    Thread.sleep(intervaloMs);
                }
                EventoProdutoRequest e = eventos.get(i);
                try {
                    EventoResposta r = servicoEvento.receber(e);
                    log.info("Simulador: {} → {}", e.produto().id(), r.resultado());
                } catch (RuntimeException erro) {
                    log.warn("Simulador: {} não processado ({})", e.produto().id(), erro.getClass().getSimpleName());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            rodando.set(false);
        }
    }
}
