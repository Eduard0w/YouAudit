package br.com.youaudit.evento;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Já há um lote rodando: um segundo lote ao mesmo tempo dobraria as requisições aos sites. Responde 409. */
@ResponseStatus(HttpStatus.CONFLICT)
public class LoteEmAndamentoException extends RuntimeException {

    public LoteEmAndamentoException() {
        super("Já existe um lote de links em andamento.");
    }
}
