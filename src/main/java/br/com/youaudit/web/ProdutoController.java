package br.com.youaudit.web;

import br.com.youaudit.coleta.StatusProduto;
import br.com.youaudit.evento.ServicoReprocessamento;
import br.com.youaudit.revisao.DetalheProduto;
import br.com.youaudit.revisao.PedidoDecisao;
import br.com.youaudit.revisao.PedidoRevisao;
import br.com.youaudit.revisao.RelatorioProdutor;
import br.com.youaudit.revisao.ServicoConsulta;
import br.com.youaudit.revisao.ServicoRevisao;
import br.com.youaudit.revisao.VersaoAuditoria;
import br.com.youaudit.regras.EstadoAchado;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Endpoints da tela de detalhe e da revisão humana (CONTRATOS.md §3). */
@RestController
public class ProdutoController {

    private final ServicoConsulta consulta;
    private final ServicoRevisao revisao;
    private final ServicoReprocessamento reprocessamento;

    public ProdutoController(ServicoConsulta consulta, ServicoRevisao revisao, ServicoReprocessamento reprocessamento) {
        this.consulta = consulta;
        this.revisao = revisao;
        this.reprocessamento = reprocessamento;
    }

    @GetMapping("/api/produtos/{id}")
    public DetalheProduto detalhe(@PathVariable String id) {
        return consulta.detalhe(id);
    }

    @GetMapping("/api/produtos/{id}/auditorias")
    public List<VersaoAuditoria> versoes(@PathVariable String id) {
        return consulta.versoes(id);
    }

    @GetMapping("/api/produtos/{id}/relatorio")
    public RelatorioProdutor relatorio(@PathVariable String id) {
        return consulta.relatorio(id);
    }

    /** Roda o pipeline na versão atual (páginas REAIS, ou auditoria que FALHOU pela IA). 202: roda em segundo plano. */
    @PostMapping("/api/produtos/{id}/processar")
    public ResponseEntity<Map<String, Object>> processar(@PathVariable String id) {
        int versao = reprocessamento.processar(id);
        return ResponseEntity.accepted().body(Map.of("produtoId", id, "versao", versao));
    }

    @PostMapping("/api/achados/{id}/revisao")
    public Map<String, EstadoAchado> revisar(@PathVariable Long id, @Valid @RequestBody PedidoRevisao pedido) {
        return Map.of("estado", revisao.revisar(id, pedido));
    }

    @PostMapping("/api/produtos/{id}/decisao")
    public Map<String, StatusProduto> decidir(@PathVariable String id, @Valid @RequestBody PedidoDecisao pedido) {
        return Map.of("status", revisao.decidir(id, pedido));
    }
}
