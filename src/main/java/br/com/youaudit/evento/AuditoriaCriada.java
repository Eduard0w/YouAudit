package br.com.youaudit.evento;

import br.com.youaudit.coleta.Origem;

/**
 * Aviso interno "uma auditoria nova está EM_ANALISE". Publicado pelo {@link ServicoEvento} e ouvido pelo
 * {@link AgendadorAuditoria} só DEPOIS do commit (senão a outra thread poderia não achar a auditoria no banco).
 */
public record AuditoriaCriada(Long auditoriaId, String produtoId, Origem origem) {
}
