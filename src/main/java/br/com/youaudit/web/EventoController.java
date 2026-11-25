package br.com.youaudit.web;

import br.com.youaudit.evento.EventoProdutoRequest;
import br.com.youaudit.evento.EventoResposta;
import br.com.youaudit.evento.ServicoEvento;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gatilho SIMULADO: no protótipo, este endpoint faz o papel do evento "produto enviado para aprovação".
 * Responde 202 Accepted: "recebi e vou processar", sem esperar a auditoria terminar.
 * {@code @Valid} faz o Spring conferir as anotações dos records e responder 400 se algo faltar.
 */
@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final ServicoEvento servicoEvento;

    public EventoController(ServicoEvento servicoEvento) {
        this.servicoEvento = servicoEvento;
    }

    @PostMapping("/produto-enviado")
    public ResponseEntity<EventoResposta> receber(@Valid @RequestBody EventoProdutoRequest pedido) {
        return ResponseEntity.accepted().body(servicoEvento.receber(pedido));
    }
}
