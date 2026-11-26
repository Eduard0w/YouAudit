package br.com.youaudit.revisao;

import br.com.youaudit.redacao.CanalNotificacao;

import java.time.Instant;

/** GET /api/produtos/{id}/relatorio: o relatório "enviado" (simulado) da auditoria mais recente. */
public record RelatorioProdutor(String produtoId, int versao, CanalNotificacao canal, Instant enviadaEm, String conteudo) {
}
