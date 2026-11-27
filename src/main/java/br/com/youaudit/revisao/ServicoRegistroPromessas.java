package br.com.youaudit.revisao;

import br.com.youaudit.coleta.Produto;
import br.com.youaudit.coleta.ProdutoRepository;
import br.com.youaudit.evento.Auditoria;
import br.com.youaudit.evento.AuditoriaRepository;
import br.com.youaudit.evento.CalculadoraHash;
import br.com.youaudit.evento.StatusAuditoria;
import br.com.youaudit.extracao.Afirmacao;
import br.com.youaudit.extracao.AfirmacaoRepository;
import br.com.youaudit.regras.Achado;
import br.com.youaudit.regras.AchadoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Peça 3 do "ciclo da promessa": no LIBERAR, congela o que foi prometido na versão aprovada; depois, mostra esse
 * registro com dois sinais recalculados pelo código (íntegro? a página mudou desde a aprovação?).
 *
 * <p>Não usa IA: só organiza dados que já estão no banco (afirmações conferidas, alertas e revisões).
 * Não é selo nem parecer jurídico: é um registro de fatos.
 */
@Service
public class ServicoRegistroPromessas {

    private final AuditoriaRepository auditorias;
    private final AfirmacaoRepository afirmacoes;
    private final AchadoRepository achados;
    private final RevisaoRepository revisoes;
    private final ProdutoRepository produtos;
    private final RegistroPromessasRepository registros;
    private final CalculadoraHash hash;
    private final ObjectMapper json;

    public ServicoRegistroPromessas(AuditoriaRepository auditorias, AfirmacaoRepository afirmacoes,
                                    AchadoRepository achados, RevisaoRepository revisoes, ProdutoRepository produtos,
                                    RegistroPromessasRepository registros, CalculadoraHash hash, ObjectMapper json) {
        this.auditorias = auditorias;
        this.afirmacoes = afirmacoes;
        this.achados = achados;
        this.revisoes = revisoes;
        this.produtos = produtos;
        this.registros = registros;
        this.hash = hash;
        this.json = json;
    }

    /**
     * Chamado pelo {@link ServicoRevisao} no LIBERAR, dentro da mesma transação da decisão: ou os dois são gravados,
     * ou nenhum. Vale a auditoria mais recente do produto, que é a que a pessoa estava vendo ao decidir.
     */
    @Transactional
    public RegistroPromessas congelar(Produto produto, String motivo, Instant momento) {
        // O banco guarda microssegundos; o relógio pode ter nanossegundos. Sem cortar, a data dentro do conteúdo
        // congelado ficaria diferente da data gravada na coluna.
        Instant data = momento.truncatedTo(ChronoUnit.MICROS);
        Auditoria a = auditorias.findFirstByProdutoIdOrderByVersaoDesc(produto.getId()).orElse(null);
        boolean concluida = a != null && a.getStatus() == StatusAuditoria.CONCLUIDA;
        String aviso = a == null ? "produto sem auditoria: promessas não extraídas"
                : concluida ? null
                : "auditoria " + a.getStatus() + ": promessas não extraídas";

        List<RegistroPromessasVisto.Promessa> promessas = a == null ? List.of()
                : afirmacoes.findByAuditoriaId(a.getId()).stream()
                .sorted(Comparator.comparing(Afirmacao::getBlocoId).thenComparing(Afirmacao::getId))
                .map(x -> new RegistroPromessasVisto.Promessa(x.getBlocoId(), x.getTipo(), x.getTrecho(), x.getValor()))
                .toList();
        List<RegistroPromessasVisto.AlertaRevisado> alertas = a == null ? List.of()
                : achados.findByAuditoriaId(a.getId()).stream()
                .sorted(Comparator.comparing(Achado::getId))
                .map(x -> new RegistroPromessasVisto.AlertaRevisado(x.getRegraCodigo(), x.getSeveridade(),
                        x.getEvidenciaPagina(), x.getEvidenciaEntrega(), x.getDetalhe(), x.getEstado(), ultimaNota(x)))
                .toList();

        int versao = a == null ? produto.getVersaoAtual() : a.getVersao();
        String hashPagina = a == null ? null : a.getHashPagina();
        var conteudo = new RegistroPromessasVisto.Conteudo(produto.getId(), produto.getNome(), produto.getOrigem(),
                versao, a == null ? null : a.getStatus(), a == null ? null : a.getPontuacao(),
                a == null ? null : a.getFaixa(), hashPagina, DecisaoProduto.LIBERAR.name(), motivo, data.toString(),
                aviso, promessas, alertas);

        String texto = escrever(conteudo);
        return registros.save(new RegistroPromessas(produto, versao, hashPagina, motivo, data, texto, hash.sha256(texto)));
    }

    @Transactional(readOnly = true)
    public List<RegistroPromessasVisto> listar(String produtoId) {
        Produto p = produtos.findById(produtoId).orElseThrow(() -> new NaoEncontradoException("produto não encontrado"));
        return registros.findByProdutoIdOrderByDataAsc(produtoId).stream().map(r -> ver(r, p)).toList();
    }

    RegistroPromessasVisto ver(RegistroPromessas r, Produto atual) {
        boolean integro = hash.sha256(r.getConteudo()).equals(r.getHashConteudo());
        Boolean paginaMudou = r.getHashPagina() == null || atual.getPaginaHash() == null ? null
                : !Objects.equals(r.getHashPagina(), atual.getPaginaHash());
        RegistroPromessasVisto.Conteudo conteudo;
        try {
            conteudo = json.readValue(r.getConteudo(), RegistroPromessasVisto.Conteudo.class);
        } catch (JsonProcessingException e) {
            conteudo = null;
            integro = false;
        }
        return new RegistroPromessasVisto(r.getId(), r.getVersao(), r.getData(), r.getMotivo(), r.getHashPagina(),
                r.getHashConteudo(), integro, paginaMudou, conteudo);
    }

    private String ultimaNota(Achado achado) {
        return revisoes.findByAchadoId(achado.getId()).stream()
                .max(Comparator.comparing(Revisao::getData))
                .map(Revisao::getNota).orElse(null);
    }

    private String escrever(RegistroPromessasVisto.Conteudo conteudo) {
        try {
            return json.writeValueAsString(conteudo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("não foi possível montar o registro das promessas", e);
        }
    }
}
