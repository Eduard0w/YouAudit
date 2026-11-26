package br.com.youaudit.evento;

import br.com.youaudit.revisao.ConflitoException;
import br.com.youaudit.revisao.NaoEncontradoException;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * POST /api/produtos/{id}/processar: roda o pipeline na auditoria mais recente, sob comando de uma pessoa.
 * É o caminho das páginas REAIS (que não rodam sozinhas) e de uma auditoria que FALHOU por causa da IA.
 *
 * <p>Aceita: EM_ANALISE (nunca rodou) ou FALHOU com página (a IA caiu; volta a EM_ANALISE e tenta de novo).
 * Recusa (409): CONCLUIDA (para auditar de novo, reenvie a página), COLETA_INSUFICIENTE e falha de coleta (sem texto).
 */
@Service
public class ServicoReprocessamento {

    private final AuditoriaRepository auditorias;
    private final ProcessadorAuditoria processador;
    private final TaskExecutor executor;

    public ServicoReprocessamento(AuditoriaRepository auditorias, ProcessadorAuditoria processador, TaskExecutor executor) {
        this.auditorias = auditorias;
        this.processador = processador;
        this.executor = executor;
    }

    @Transactional
    public int processar(String produtoId) {
        Auditoria a = auditorias.findFirstByProdutoIdOrderByVersaoDesc(produtoId)
                .orElseThrow(() -> new NaoEncontradoException("produto sem auditoria"));
        if (a.getStatus() == StatusAuditoria.FALHOU && a.getHashPagina() != null) {
            a.setStatus(StatusAuditoria.EM_ANALISE);
            a.setMotivoFalha(null);
            a.setConcluidaEm(null);
        }
        if (a.getStatus() != StatusAuditoria.EM_ANALISE) {
            throw new ConflitoException("a auditoria está " + a.getStatus() + "; para auditar de novo, reenvie a página");
        }
        Long id = a.getId();
        // Agenda só depois do commit, para a outra thread já ver a auditoria EM_ANALISE.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                executor.execute(() -> processador.processar(id));
            }
        });
        return a.getVersao();
    }
}
