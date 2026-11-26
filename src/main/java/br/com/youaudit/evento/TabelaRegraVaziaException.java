package br.com.youaudit.evento;

/** Nenhuma regra ativa na tabela regra: a auditoria FALHA com este motivo em vez de pontuar 0 em silêncio. */
public class TabelaRegraVaziaException extends RuntimeException {

    public TabelaRegraVaziaException() {
        super("nenhuma regra ativa na tabela regra (o data.sql rodou?): auditoria não pontuada");
    }
}
