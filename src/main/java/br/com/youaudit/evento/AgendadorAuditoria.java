package br.com.youaudit.evento;

import br.com.youaudit.coleta.Origem;
import br.com.youaudit.config.PipelinePropriedades;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Faz o pipeline ser assíncrono: quando uma auditoria é criada, agenda o processamento no pool de threads e o
 * evento já respondeu 202. {@code AFTER_COMMIT}: só agenda depois que produto, evento e auditoria estão gravados.
 *
 * <p>Origem REAL só é agendada se {@code pipeline.processar-real-automaticamente=true} (ver {@link PipelinePropriedades}).
 */
@Component
public class AgendadorAuditoria {

    private static final Logger log = LoggerFactory.getLogger(AgendadorAuditoria.class);

    private final ProcessadorAuditoria processador;
    private final TaskExecutor executor;
    private final PipelinePropriedades propriedades;

    public AgendadorAuditoria(ProcessadorAuditoria processador, TaskExecutor executor, PipelinePropriedades propriedades) {
        this.processador = processador;
        this.executor = executor;
        this.propriedades = propriedades;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoCriar(AuditoriaCriada evento) {
        if (evento.origem() == Origem.REAL && !propriedades.processarRealAutomaticamente()) {
            log.info("Auditoria {} ({}): origem REAL, fica EM_ANALISE até POST /api/produtos/{}/processar",
                    evento.auditoriaId(), evento.produtoId(), evento.produtoId());
            return;
        }
        executor.execute(() -> processador.processar(evento.auditoriaId()));
    }
}
