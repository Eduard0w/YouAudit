package br.com.youaudit.revisao;

import br.com.youaudit.evento.StatusAuditoria;
import br.com.youaudit.pontuacao.Faixa;
import br.com.youaudit.regras.TipoAchado;

import java.time.Instant;
import java.util.List;

/** Uma linha de GET /api/produtos/{id}/auditorias: para ver o score mudando entre versões (reenvio). */
public record VersaoAuditoria(int versao, StatusAuditoria status, Integer pontuacao, Faixa faixa,
                              List<TipoAchado> tiposDeAchado, Instant criadaEm) {
}
