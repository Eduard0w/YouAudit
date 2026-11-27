package br.com.youaudit.revisao;

import br.com.youaudit.coleta.ConteudoItemRepository;
import br.com.youaudit.coleta.Produto;
import br.com.youaudit.coleta.ProdutoRepository;
import br.com.youaudit.entrega.DescarteCruzamentoRepository;
import br.com.youaudit.evento.Auditoria;
import br.com.youaudit.evento.AuditoriaRepository;
import br.com.youaudit.evento.StatusAuditoria;
import br.com.youaudit.extracao.AfirmacaoDescartadaRepository;
import br.com.youaudit.extracao.AfirmacaoRepository;
import br.com.youaudit.pontuacao.Faixa;
import br.com.youaudit.redacao.NotificacaoProdutorRepository;
import br.com.youaudit.regras.Achado;
import br.com.youaudit.regras.AchadoRepository;
import br.com.youaudit.regras.EstadoAchado;
import br.com.youaudit.regras.Regra;
import br.com.youaudit.regras.RegraRepository;
import br.com.youaudit.regras.Severidade;
import br.com.youaudit.regras.TipoAchado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Optional;
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
    private final RevisaoRepository revisoes;
    private final ComparadorVersoes comparador;

    public ServicoConsulta(ProdutoRepository produtos, AuditoriaRepository auditorias, AchadoRepository achados,
                           AfirmacaoRepository afirmacoes, AfirmacaoDescartadaRepository afirmacoesDescartadas,
                           DescarteCruzamentoRepository descartesCruzamento, ConteudoItemRepository conteudos,
                           RegistroDecisaoRepository decisoes, RegraRepository regras,
                           NotificacaoProdutorRepository notificacoes, RevisaoRepository revisoes,
                           ComparadorVersoes comparador) {
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
        this.revisoes = revisoes;
        this.comparador = comparador;
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

    /**
     * Compara a versão mais recente com a anterior. As duas precisam estar CONCLUIDAS: comparar com uma auditoria que
     * falhou ou ainda roda daria "tudo resolvido" ou "tudo novo" sem ser verdade.
     */
    @Transactional(readOnly = true)
    public ComparacaoVersoes comparacao(String produtoId) {
        Produto p = produto(produtoId);
        Auditoria atual = auditorias.findFirstByProdutoIdOrderByVersaoDesc(produtoId)
                .orElseThrow(() -> new NaoEncontradoException("o produto não tem auditoria"));
        Auditoria anterior = auditorias.findByProdutoIdOrderByVersaoAsc(produtoId).stream()
                .filter(a -> a.getVersao() == atual.getVersao() - 1).findFirst()
                .orElseThrow(() -> new NaoEncontradoException("não há versão anterior para comparar"));
        if (atual.getStatus() != StatusAuditoria.CONCLUIDA || anterior.getStatus() != StatusAuditoria.CONCLUIDA) {
            throw new NaoEncontradoException("as duas versões precisam estar concluídas para comparar");
        }

        // A decisão é guardada por produto, não por versão: vale a última registrada entre as duas versões.
        Optional<RegistroDecisao> decisao = decisoes.findByProdutoIdOrderByDataAsc(produtoId).stream()
                .filter(d -> !d.getData().isBefore(anterior.getCriadaEm()) && d.getData().isBefore(atual.getCriadaEm()))
                .reduce((primeira, segunda) -> segunda);
        boolean pedidoAjuste = decisao.map(d -> d.getDecisao() == DecisaoProduto.PEDIR_AJUSTE).orElse(false);

        List<ComparadorVersoes.Item> itens = comparador.comparar(alertas(anterior), alertas(atual), pedidoAjuste);
        return new ComparacaoVersoes(p.getId(), p.getOrigem(),
                anterior.getVersao(), anterior.getPontuacao(), anterior.getFaixa(),
                atual.getVersao(), atual.getPontuacao(), atual.getFaixa(),
                decisao.map(d -> new DetalheProduto.DecisaoVista(d.getDecisao(), d.getMotivo(), d.getData())).orElse(null),
                pedidoAjuste, itens, contar(itens));
    }

    private List<ComparadorVersoes.Alerta> alertas(Auditoria a) {
        return achados.findByAuditoriaId(a.getId()).stream()
                .sorted(Comparator.comparing(Achado::getId))
                .map(x -> new ComparadorVersoes.Alerta(x.getId(), x.getRegraCodigo(), x.getEvidenciaPagina(),
                        x.getEvidenciaEntrega(), x.getDetalhe(), x.getEstado(), ultimaNota(x.getId())))
                .toList();
    }

    private String ultimaNota(Long achadoId) {
        return revisoes.findByAchadoId(achadoId).stream()
                .max(Comparator.comparing(Revisao::getData))
                .map(Revisao::getNota).orElse(null);
    }

    static ComparacaoVersoes.Contagem contar(List<ComparadorVersoes.Item> itens) {
        int resolvidos = 0, continuam = 0, iguais = 0, novos = 0, anterior = 0, atual = 0, semJulgamento = 0;
        for (ComparadorVersoes.Item i : itens) {
            if (i.anterior() != null) anterior++;
            if (i.atual() != null) atual++;
            switch (i.situacao()) {
                case RESOLVIDO -> resolvidos++;
                case CONTINUA -> continuam++;
                case IGUAL_ANTERIOR -> iguais++;
                case NOVO -> novos++;
            }
            boolean nuncaJulgado = i.situacao() == SituacaoAlerta.NOVO
                    || (i.atual() != null && i.anterior().estado() == EstadoAchado.PENDENTE);
            if (nuncaJulgado) semJulgamento++;
        }
        return new ComparacaoVersoes.Contagem(resolvidos, continuam, iguais, novos, anterior, atual, semJulgamento);
    }

    private Produto produto(String id) {
        return produtos.findById(id).orElseThrow(() -> new NaoEncontradoException("produto não encontrado"));
    }
}
