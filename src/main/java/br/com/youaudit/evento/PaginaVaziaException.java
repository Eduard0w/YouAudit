package br.com.youaudit.evento;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** A página chegou, mas depois de tirar o HTML não sobrou texto para auditar. Responde 400. */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PaginaVaziaException extends RuntimeException {

    public PaginaVaziaException(String produtoId) {
        super("A página do produto " + produtoId + " não tem texto depois de remover o HTML.");
    }
}
