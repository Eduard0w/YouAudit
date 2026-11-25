package br.com.youaudit.evento;

import java.util.List;

/** Resposta do POST /api/lote/urls: quantos eventos foram agendados e quais linhas do arquivo foram ignoradas. */
public record RespostaLote(int eventosAgendados, List<Integer> linhasInvalidas, long intervaloMs) {
}
