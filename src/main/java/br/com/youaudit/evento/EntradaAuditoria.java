package br.com.youaudit.evento;

/** Por onde a auditoria entrou. Aparece como etiqueta na fila. */
public enum EntradaAuditoria {
    /** Gatilho (simulado) "produto enviado para aprovação", inclusive o lote de data/real/urls.txt. */
    EVENTO,
    /** Alguém colou um link no formulário "Auditar um link". */
    LINK
}
