package br.com.youaudit.revisao;

import br.com.youaudit.coleta.Origem;
import br.com.youaudit.coleta.StatusProduto;
import br.com.youaudit.coleta.TipoConteudo;
import br.com.youaudit.evento.EntradaAuditoria;
import br.com.youaudit.evento.StatusAuditoria;
import br.com.youaudit.extracao.TipoAfirmacao;
import br.com.youaudit.pontuacao.Faixa;
import br.com.youaudit.regras.EstadoAchado;
import br.com.youaudit.regras.Severidade;
import br.com.youaudit.regras.TipoAchado;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * GET /api/produtos/{id}: tudo o que a tela de detalhe mostra, da auditoria MAIS RECENTE. Nunca tem URL (regra 8).
 *
 * @param liberacaoSugerida faixa BAIXA e nenhum achado de severidade ALTA (CONTRATOS.md §4). É só uma sugestão na
 *                          tela: o sistema NUNCA libera sozinho
 * @param conta             o peso de cada tipo de achado que entrou na pontuação (para mostrar "20 + 15 + ...")
 */
public record DetalheProduto(String id, String nome, Origem origem, String categoria, BigDecimal preco,
                             StatusProduto status, int versaoAtual, Auditoria auditoria, String paginaTexto,
                             List<AchadoVisto> achados, List<Parcela> conta, List<AfirmacaoVista> afirmacoes,
                             int afirmacoesDescartadas, int descartesCruzamento, List<ItemVisto> entrega,
                             List<DecisaoVista> decisoes, boolean liberacaoSugerida) {

    public record Auditoria(Long id, int versao, StatusAuditoria status, EntradaAuditoria entrada, Integer pontuacao,
                            Faixa faixa, String motivoFalha, Boolean cruzamentoPulado, Instant criadaEm,
                            Instant concluidaEm) {
    }

    public record AchadoVisto(Long id, TipoAchado regra, String regraDescricao, int peso, Severidade severidade,
                              String evidenciaPagina, String evidenciaEntrega, String detalhe, String sugestao,
                              String sugestaoFonte, EstadoAchado estado) {
    }

    public record Parcela(TipoAchado regra, int peso) {
    }

    public record AfirmacaoVista(int blocoId, TipoAfirmacao tipo, String trecho, double confianca) {
    }

    public record ItemVisto(TipoConteudo tipo, String titulo, String descricao, Integer duracaoMin) {
    }

    public record DecisaoVista(DecisaoProduto decisao, String motivo, Instant data) {
    }
}
