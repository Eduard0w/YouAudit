package br.com.youaudit.web;

import br.com.youaudit.evento.RespostaLote;
import br.com.youaudit.evento.ServicoLote;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** POST /api/lote/urls: lê data/real/urls.txt e dispara um evento por link, em segundo plano. Responde 202. */
@RestController
public class LoteController {

    private final ServicoLote servicoLote;

    public LoteController(ServicoLote servicoLote) {
        this.servicoLote = servicoLote;
    }

    @PostMapping("/api/lote/urls")
    public ResponseEntity<RespostaLote> iniciar() {
        return ResponseEntity.accepted().body(servicoLote.iniciar());
    }
}
