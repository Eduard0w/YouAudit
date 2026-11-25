package br.com.youaudit.evento;

/**
 * Resposta do endpoint de evento. {@code auditoriaId}, {@code versao} e {@code statusAuditoria} ficam nulos
 * quando o evento foi ignorado. {@code produtoId} é útil com entrada LINK, em que o id é atribuído pelo sistema.
 */
public record EventoResposta(Long eventoId, String produtoId, ResultadoEvento resultado, Long auditoriaId,
                             Integer versao, StatusAuditoria statusAuditoria, EntradaAuditoria entrada) {
}
