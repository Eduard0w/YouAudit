package br.com.youaudit.revisao;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Pedido que não cabe no estado atual (ex.: reprocessar auditoria já concluída): vira 409. */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
