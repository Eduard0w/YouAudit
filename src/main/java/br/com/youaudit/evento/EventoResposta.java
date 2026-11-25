package br.com.youaudit.evento;

/**
 * Resposta do endpoint de evento. {@code auditoriaId} e {@code versao} ficam nulos quando o evento foi ignorado.
 */
public record EventoResposta(Long eventoId, ResultadoEvento resultado, Long auditoriaId, Integer versao) {
}
