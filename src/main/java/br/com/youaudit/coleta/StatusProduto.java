package br.com.youaudit.coleta;

/**
 * Situação do produto no fluxo de aprovação.
 * Não estava no CONTRATOS.md §1; foi adicionado na implementação (ver o contrato).
 */
public enum StatusProduto {
    /** Há uma auditoria rodando para a versão atual. */
    EM_ANALISE,
    /** A auditoria terminou e espera uma pessoa de Operações. */
    AGUARDANDO_DECISAO,
    LIBERADO,
    AJUSTE_PEDIDO,
    ESCALADO
}
