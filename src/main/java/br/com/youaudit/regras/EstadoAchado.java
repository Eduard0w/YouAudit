package br.com.youaudit.regras;

/**
 * Situação de um achado. Começa PENDENTE; muda quando uma pessoa revisa.
 * Não estava enumerado no CONTRATOS.md §1; foi adicionado na implementação.
 */
public enum EstadoAchado {
    PENDENTE,
    CONFIRMADO,
    DESCARTADO
}
