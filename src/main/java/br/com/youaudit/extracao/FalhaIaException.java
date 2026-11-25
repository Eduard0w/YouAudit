package br.com.youaudit.extracao;

/**
 * A IA não devolveu resposta utilizável. A mensagem vai para o banco como motivo da falha da auditoria,
 * por isso NUNCA contém a chave da API nem o texto enviado.
 */
public class FalhaIaException extends RuntimeException {

    public FalhaIaException(String motivo) {
        super(motivo);
    }
}
