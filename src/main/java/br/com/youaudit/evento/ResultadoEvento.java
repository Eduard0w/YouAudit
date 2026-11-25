package br.com.youaudit.evento;

/** O que o sistema fez com um evento recebido. */
public enum ResultadoEvento {
    /** Gerou uma nova auditoria. */
    PROCESSADO,
    /** A página é igual à da última auditoria válida: nada a fazer (idempotência). */
    IGNORADO_MESMO_HASH
}
