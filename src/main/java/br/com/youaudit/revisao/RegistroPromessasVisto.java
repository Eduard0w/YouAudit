package br.com.youaudit.revisao;

import br.com.youaudit.coleta.Origem;
import br.com.youaudit.evento.StatusAuditoria;
import br.com.youaudit.extracao.TipoAfirmacao;
import br.com.youaudit.pontuacao.Faixa;
import br.com.youaudit.regras.EstadoAchado;
import br.com.youaudit.regras.Severidade;
import br.com.youaudit.regras.TipoAchado;

import java.time.Instant;
import java.util.List;

/**
 * Uma linha de GET /api/produtos/{id}/registros.
 *
 * @param integro                  o SHA-256 do conteúdo guardado, recalculado agora, bate com o hash gravado
 * @param paginaMudouDesdeAprovacao o hash da página ATUAL do produto é diferente do hash da página aprovada; nulo
 *                                  quando um dos dois hashes não existe (o código não sabe dizer)
 * @param conteudo                 o que foi congelado; nulo se o conteúdo não pôde ser lido (aí {@code integro} é falso)
 */
public record RegistroPromessasVisto(Long id, int versao, Instant data, String motivo, String hashPagina,
                                     String hashConteudo, boolean integro, Boolean paginaMudouDesdeAprovacao,
                                     Conteudo conteudo) {

    /**
     * O JSON congelado. A ordem dos campos é a da declaração (Jackson serializa records assim), então o mesmo
     * registro sempre vira o mesmo texto.
     *
     * @param aviso preenchido quando a auditoria aprovada não foi concluída (ex.: FALHOU): as promessas não foram extraídas
     */
    public record Conteudo(String produtoId, String nome, Origem origem, int versao, StatusAuditoria statusAuditoria,
                           Integer pontuacao, Faixa faixa, String hashPagina, String decisao, String motivo,
                           String data, String aviso, List<Promessa> promessas, List<AlertaRevisado> alertas) {
    }

    /** Uma afirmação que passou pela checagem de trecho literal (todos os tipos, inclusive OUTRO). */
    public record Promessa(int blocoId, TipoAfirmacao tipo, String trecho, String valor) {
    }

    /** Um alerta da versão aprovada, com o julgamento da pessoa naquele momento. */
    public record AlertaRevisado(TipoAchado tipo, Severidade severidade, String evidenciaPagina,
                                 String evidenciaEntrega, String detalhe, EstadoAchado estado, String nota) {
    }
}
