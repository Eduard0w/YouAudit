package br.com.youaudit.web;

import br.com.youaudit.evento.EventoResposta;
import br.com.youaudit.simulador.SimuladorEventos;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Gatilho SIMULADO da demo: dispara os produtos fictícios (CONTRATOS.md §3) e o reenvio corrigido. */
@RestController
public class SimuladorController {

    private final SimuladorEventos simulador;

    public SimuladorController(SimuladorEventos simulador) {
        this.simulador = simulador;
    }

    @PostMapping("/api/simulador/iniciar")
    public ResponseEntity<Map<String, Object>> iniciar() {
        return ResponseEntity.accepted().body(Map.of("eventosAgendados", simulador.iniciar(), "origem", "SIMULADO"));
    }

    @PostMapping("/api/simulador/reenviar/{id}")
    public ResponseEntity<EventoResposta> reenviar(@PathVariable String id) {
        return ResponseEntity.accepted().body(simulador.reenviar(id));
    }
}
