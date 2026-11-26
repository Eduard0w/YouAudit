package br.com.youaudit.revisao;

import br.com.youaudit.coleta.ConteudoItemRepository;
import br.com.youaudit.coleta.Produto;
import br.com.youaudit.coleta.ProdutoRepository;
import br.com.youaudit.entrega.DescarteCruzamentoRepository;
import br.com.youaudit.evento.Auditoria;
import br.com.youaudit.evento.AuditoriaRepository;
import br.com.youaudit.extracao.AfirmacaoDescartadaRepository;
import br.com.youaudit.extracao.AfirmacaoRepository;
import br.com.youaudit.pontuacao.Faixa;
import br.com.youaudit.redacao.NotificacaoProdutorRepository;
import br.com.youaudit.regras.Achado;
import br.com.youaudit.regras.AchadoRepository;
import br.com.youaudit.regras.Regra;
import br.com.youaudit.regras.RegraRepository;
import br.com.youaudit.regras.Severidade;
import br.com.youaudit.regras.TipoAchado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Leituras para a tela (detalhe, versões, relatório). Só lê: não muda nada no banco. */
@Service
public class ServicoConsulta {

    private final ProdutoRepository produtos;
    private final AuditoriaRepository auditorias;
    private final AchadoRepository achados;
    private final AfirmacaoRepository afirmacoes;
    private final AfirmacaoDescartadaRepository afirmacoesDescartadas;
    private final DescarteCruzamentoRepository descartesCruzamento;
    private final ConteudoItemRepository conteudos;
    private final RegistroDecisaoRepository decisoes;
    private final RegraRepository regras;
    private final NotificacaoProdutorRepository notificacoes;

    public ServicoConsulta(ProdutoRepository produtos, AuditoriaRepository auditorias, AchadoRepository achados,
                           AfirmacaoRepository afirmacoes, AfirmacaoDescartadaRepository afirmacoesDescartadas,
                           DescarteCruzamentoRepository descartesCruzamento, ConteudoItemRepository conteudos,
                           RegistroDecisaoRepository decisoes, RegraRepository regras,
                           NotificacaoProdutorRepository notificacoes) {
        this.produtos = produtos;
        this.auditorias = auditorias;
        this.achados = achados;
        this.afirmacoes = afirmacoes;
        this.afirmacoesDescartadas = afirmacoesDescartadas;
        this.descartesCruzamento = descartesCruzamento;
        this.conteudos = conteudos;
        this.decisoes = decisoes;
        this.regras = regras;
        this.notificacoes = notificacoes;
    }

    @Transactional(readOnly = true)
    public DetalheProduto detalhe(String produtoId) {
        Produto p = produto(produtoId);
        Auditoria a = auditorias.findFirstByProdutoIdOrderByVersaoDesc(produtoId)
                .orElseThrow(() -> new NaoEncontradoException("o produto não tem auditoria"));
        Map<TipoAchado, Regra> porCodigo = new EnumMap<>(TipoAchado.class);
        regras.findAll().forEach(r -> porCodigo.put(r.getCodigo(), r));

        List<Achado> lista = achados.findByAuditoriaId(a.getId()).stream()
                .sorted(Comparator.comparing(Achado::getId)).toList();
        List<DetalheProduto.AchadoVisto> vistos = lista.stream().map(x -> {
            Regra r = porCodigo.get(x.getRegraCodigo());
            return new DetalheProduto.AchadoVisto(x.getId(), x.getRegraCodigo(), r == null ? null : r.getDescricao(),
                    r == null ? 0 : r.getPeso(), x.getSeveridade(), x.getEvidenciaPagina(), x.getEvidenciaEntrega(),
                    x.getDetalhe(), x.getSugestao(), x.getSugestaoFonte(), x.getEstado());
        }).toList();
        List<DetalheProduto.Parcela> conta = lista.stream().map(Achado::getRegraCodigo).distinct().sorted()
                .map(t -> new DetalheProduto.Parcela(t, porCodigo.containsKey(t) ? porCodigo.get(t).getPeso() : 0)).toList();
        boolean liberacaoSugerida = a.getFaixa() == Faixa.BAIXA
                && lista.stream().noneMatch(x -> x.getSeveridade() == Severidade.ALTA);

        return new DetalheProduto(p.getId(), p.getNome(), p.getOrigem(), p.getCategoria(), p.getPreco(), p.getStatus(),
                p.getVersaoAtual(),
                new DetalheProduto.Auditoria(a.getId(), a.getVersao(), a.getStatus(), a.getEntrada(), a.getPontuacao(),
                        a.getFaixa(), a.getMotivoFalha(), a.getCruzamentoPulado(), a.getCriadaEm(), a.getConcluidaEm()),
                p.getPaginaTexto(), vistos, conta,
                afirmacoes.findByAuditoriaId(a.getId()).stream()
                        .map(x -> new DetalheProduto.AfirmacaoVista(x.getBlocoId(), x.getTipo(), x.getTrecho(), x.getConfianca()))
                        .toList(),
                afirmacoesDescartadas.findByAuditoriaId(a.getId()).size(),
                descartesCruzamento.findByAuditoriaId(a.getId()).size(),
                conteudos.findByProdutoId(produtoId).stream()
                        .map(i -> new DetalheProduto.ItemVisto(i.getTipo(), i.getTitulo(), i.getDescricao(), i.getDuracaoMin()))
                        .toList(),
                decisoes.findByProdutoIdOrderByDataAsc(produtoId).stream()
                        .map(d -> new DetalheProduto.DecisaoVista(d.getDecisao(), d.getMotivo(), d.getData())).toList(),
                liberacaoSugerida);
    }

    @Transactional(readOnly = true)
    public List<VersaoAuditoria> versoes(String produtoId) {
        produto(produtoId);
        return auditorias.findByProdutoIdOrderByVersaoAsc(produtoId).stream()
                .map(a -> new VersaoAuditoria(a.getVersao(), a.getStatus(), a.getPontuacao(), a.getFaixa(),
                        achados.findByAuditoriaId(a.getId()).stream().map(Achado::getRegraCodigo).distinct().sorted().toList(),
                        a.getCriadaEm()))
                .toList();
    }

    @Transactional(readOnly = true)
    public RelatorioProdutor relatorio(String produtoId) {
        produto(produtoId);
        Auditoria a = auditorias.findFirstByProdutoIdOrderByVersaoDesc(produtoId)
                .orElseThrow(() -> new NaoEncontradoException("o produto não tem auditoria"));
        return notificacoes.findByAuditoriaId(a.getId()).stream().findFirst()
                .map(n -> new RelatorioProdutor(produtoId, a.getVersao(), n.getCanal(), n.getEnviadaEm(), n.getConteudo()))
                .orElseThrow(() -> new NaoEncontradoException("ainda não há relatório para a versão " + a.getVersao()));
    }

    private Produto produto(String id) {
        return produtos.findById(id).orElseThrow(() -> new NaoEncontradoException("produto não encontrado"));
    }
}
