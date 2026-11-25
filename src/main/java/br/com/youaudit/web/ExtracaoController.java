package br.com.youaudit.web;

import br.com.youaudit.coleta.Produto;
import br.com.youaudit.coleta.ProdutoRepository;
import br.com.youaudit.evento.Auditoria;
import br.com.youaudit.evento.AuditoriaRepository;
import br.com.youaudit.extracao.FalhaIaException;
import br.com.youaudit.extracao.ResultadoExtracao;
import br.com.youaudit.extracao.ServicoExtracao;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * POST /api/produtos/{id}/extrair — SÓ PARA DESENVOLVIMENTO: roda a extração na versão atual do produto e devolve
 * o resultado, para revisarmos o JSON juntos (PLANO.md, Dia 1). No fluxo normal, a extração vai rodar dentro do
 * pipeline assíncrono, disparada pelo evento.
 */
@RestController
public class ExtracaoController {

    private final ProdutoRepository produtos;
    private final AuditoriaRepository auditorias;
    private final ServicoExtracao servicoExtracao;

    public ExtracaoController(ProdutoRepository produtos, AuditoriaRepository auditorias,
                              ServicoExtracao servicoExtracao) {
        this.produtos = produtos;
        this.auditorias = auditorias;
        this.servicoExtracao = servicoExtracao;
    }

    @PostMapping("/api/produtos/{id}/extrair")
    public ExtracaoResposta extrair(@PathVariable String id) {
        Produto produto = produtos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "produto não encontrado"));
        if (produto.getPaginaTexto() == null || produto.getPaginaTexto().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "o produto ainda não tem texto de página");
        }
        Auditoria auditoria = auditorias.findFirstByProdutoIdOrderByVersaoDesc(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "o produto não tem auditoria"));

        ResultadoExtracao r = servicoExtracao.extrair(auditoria, produto.getPaginaTexto());
        return new ExtracaoResposta(id, auditoria.getId(), auditoria.getVersao(), r.blocos(), r.chamadasIa(),
                r.respostasDoCache(), r.aceitas().size(), r.descartadas().size(),
                r.aceitas().stream().map(a -> new ExtracaoResposta.Aceita(a.getBlocoId(), a.getTipo().name(),
                        a.getTrecho(), a.getValor(), a.getConfianca())).toList(),
                r.descartadas().stream().map(d -> new ExtracaoResposta.Descartada(d.getBlocoId(), d.getTipo(),
                        d.getTrecho(), d.getMotivo(), d.getTentativa())).toList());
    }

    /** IA indisponível: 503 com o motivo (que nunca contém a chave). */
    @ExceptionHandler(FalhaIaException.class)
    public ResponseEntity<Map<String, String>> iaIndisponivel(FalhaIaException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("motivo", e.getMessage()));
    }
}
