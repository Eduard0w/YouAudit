package br.com.youaudit.revisao;

import br.com.youaudit.coleta.Origem;
import br.com.youaudit.coleta.StatusProduto;
import br.com.youaudit.evento.EntradaAuditoria;
import br.com.youaudit.evento.StatusAuditoria;
import br.com.youaudit.pontuacao.Faixa;

import java.time.Instant;

/**
 * Uma linha da fila de Operações: o produto e a sua auditoria mais recente.
 * {@code pontuacao} e {@code faixa} ficam nulas enquanto a pontuação não for calculada (ainda não existe).
 * Não há URL aqui, nunca: o produto aparece só pelo id anônimo.
 */
public record ItemFila(
        String produtoId,
        String nome,
        Origem origem,
        EntradaAuditoria entrada,
        int versao,
        StatusAuditoria statusAuditoria,
        StatusProduto statusProduto,
        Integer pontuacao,
        Faixa faixa,
        String motivoFalha,
        Instant criadaEm) {
}
