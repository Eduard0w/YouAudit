package br.com.youaudit.evento;

public enum StatusAuditoria {
    EM_ANALISE,
    CONCLUIDA,
    FALHOU,
    /** A página foi baixada, mas o texto limpo é curto demais para auditar (ver coleta.minimo-caracteres). */
    COLETA_INSUFICIENTE
}
