package br.com.youaudit.evento;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** O arquivo de links não existe ou não pôde ser lido. Responde 404. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ArquivoUrlsIndisponivelException extends RuntimeException {

    public ArquivoUrlsIndisponivelException() {
        super("Arquivo de links (lote.arquivo-urls) não encontrado ou ilegível.");
    }
}
