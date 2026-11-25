package br.com.youaudit.regras;

/**
 * Gravidade de um achado. Usada na política "faixa BAIXA sem achado de severidade ALTA → liberação sugerida".
 * Não estava enumerada no CONTRATOS.md §1; foi adicionada na implementação.
 */
public enum Severidade {
    BAIXA,
    MEDIA,
    ALTA
}
