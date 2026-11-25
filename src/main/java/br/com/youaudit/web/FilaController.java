package br.com.youaudit.web;

import br.com.youaudit.revisao.ItemFila;
import br.com.youaudit.revisao.ServicoFila;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** GET /api/fila: produtos ordenados por pontuação, com a versão e a etiqueta de entrada da auditoria. */
@RestController
public class FilaController {

    private final ServicoFila servicoFila;

    public FilaController(ServicoFila servicoFila) {
        this.servicoFila = servicoFila;
    }

    @GetMapping("/api/fila")
    public List<ItemFila> fila() {
        return servicoFila.listar();
    }
}
