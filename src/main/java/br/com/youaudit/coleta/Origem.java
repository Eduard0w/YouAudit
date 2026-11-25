package br.com.youaudit.coleta;

/** De onde vem o dado. REAL e SIMULADO nunca se misturam numa mesma métrica (regra 7 do CLAUDE.md). */
public enum Origem {
    REAL,
    SIMULADO
}
