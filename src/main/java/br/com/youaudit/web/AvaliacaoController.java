package br.com.youaudit.web;

import br.com.youaudit.avaliacao.Comparativo;
import br.com.youaudit.avaliacao.ServicoComparativo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * GET /api/avaliacao/comparativo?divisao=DESENVOLVIMENTO. RESERVADO só deve ser pedido UMA vez, no Dia 3, depois do
 * congelamento (PLANO.md). O endpoint só calcula sobre o que já está no banco: quem roda a IA é o "Processar".
 */
@RestController
public class AvaliacaoController {

    private final ServicoComparativo servico;

    public AvaliacaoController(ServicoComparativo servico) {
        this.servico = servico;
    }

    @GetMapping("/api/avaliacao/comparativo")
    public Comparativo comparativo(@RequestParam(defaultValue = "DESENVOLVIMENTO") String divisao,
                                   @RequestParam(defaultValue = "false") boolean incluirOutro) {
        if (!divisao.equals("DESENVOLVIMENTO") && !divisao.equals("RESERVADO")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "divisao deve ser DESENVOLVIMENTO ou RESERVADO");
        }
        return servico.calcular(divisao, incluirOutro);
    }
}
