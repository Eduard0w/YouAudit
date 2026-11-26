package br.com.youaudit.evento;

import br.com.youaudit.avaliacao.AchadoDetectado;
import br.com.youaudit.avaliacao.AfirmacaoDetectada;
import br.com.youaudit.avaliacao.ItemEntrega;
import br.com.youaudit.coleta.ConteudoItem;
import br.com.youaudit.coleta.ConteudoItemRepository;
import br.com.youaudit.coleta.Produto;
import br.com.youaudit.coleta.StatusProduto;
import br.com.youaudit.entrega.ResultadoCruzamento;
import br.com.youaudit.entrega.ServicoCruzamento;
import br.com.youaudit.extracao.Afirmacao;
import br.com.youaudit.extracao.AfirmacaoRepository;
import br.com.youaudit.extracao.FalhaIaException;
import br.com.youaudit.extracao.PreparadorBlocos;
import br.com.youaudit.extracao.ServicoExtracao;
import br.com.youaudit.pontuacao.CalculadoraPontuacao;
import br.com.youaudit.pontuacao.ResultadoPontuacao;
import br.com.youaudit.redacao.MontadorRelatorio;
import br.com.youaudit.redacao.NotificacaoProdutor;
import br.com.youaudit.redacao.NotificacaoProdutorRepository;
import br.com.youaudit.redacao.ServicoRedacao;
import br.com.youaudit.redacao.Sugestao;
import br.com.youaudit.regras.Achado;
import br.com.youaudit.regras.AchadoRepository;
import br.com.youaudit.regras.ContextoRegras;
import br.com.youaudit.regras.MotorRegras;
import br.com.youaudit.regras.Regra;
import br.com.youaudit.regras.RegraRepository;
import br.com.youaudit.regras.TipoAchado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Passos 3 a 9 do pipeline (CONTRATOS.md §4) para UMA auditoria, em segundo plano:
 * extração (IA) → regras (código) → cruzamento (IA + código) → pontuação (código) → redação (IA + código) →
 * relatório do produtor (simulado). No fim, a auditoria fica CONCLUIDA e o produto AGUARDANDO_DECISAO de uma pessoa.
 *
 * <p>Duas transações, de propósito:
 * <ol>
 *   <li>a extração, sozinha: as afirmações e o cache da IA ficam gravados mesmo se o cruzamento falhar depois
 *       (não gasta a cota de novo ao reprocessar);</li>
 *   <li>o resto: achados, pontuação e relatório gravados juntos, ou nada.</li>
 * </ol>
 * Falha da IA na extração ou no cruzamento → auditoria FALHOU com o motivo, produto AGUARDANDO_DECISAO (nunca fica
 * preso; reenviar ou reprocessar tenta de novo). Falha na redação NÃO derruba: usa o texto de reserva.
 *
 * <p>Regra de ouro: nada aqui aprova ou reprova. A pontuação ordena a fila; quem decide é a pessoa.
 */
@Service
public class ProcessadorAuditoria {

    private static final Logger log = LoggerFactory.getLogger(ProcessadorAuditoria.class);

    private final AuditoriaRepository auditorias;
    private final ConteudoItemRepository conteudos;
    private final ServicoExtracao extracao;
    private final AfirmacaoRepository afirmacoes;
    private final PreparadorBlocos preparador;
    private final MotorRegras motorRegras;
    private final ServicoCruzamento cruzamento;
    private final RegraRepository regras;
    private final CalculadoraPontuacao calculadora;
    private final ServicoRedacao redacao;
    private final MontadorRelatorio montadorRelatorio;
    private final AchadoRepository achados;
    private final NotificacaoProdutorRepository notificacoes;
    private final TransactionTemplate transacao;
    private final Clock relogio;

    public ProcessadorAuditoria(AuditoriaRepository auditorias, ConteudoItemRepository conteudos,
                                ServicoExtracao extracao, AfirmacaoRepository afirmacoes, PreparadorBlocos preparador,
                                MotorRegras motorRegras, ServicoCruzamento cruzamento, RegraRepository regras,
                                CalculadoraPontuacao calculadora, ServicoRedacao redacao,
                                MontadorRelatorio montadorRelatorio, AchadoRepository achados,
                                NotificacaoProdutorRepository notificacoes, PlatformTransactionManager transacoes,
                                Clock relogio) {
        this.auditorias = auditorias;
        this.conteudos = conteudos;
        this.extracao = extracao;
        this.afirmacoes = afirmacoes;
        this.preparador = preparador;
        this.motorRegras = motorRegras;
        this.cruzamento = cruzamento;
        this.regras = regras;
        this.calculadora = calculadora;
        this.redacao = redacao;
        this.montadorRelatorio = montadorRelatorio;
        this.achados = achados;
        this.notificacoes = notificacoes;
        this.transacao = new TransactionTemplate(transacoes);
        this.relogio = relogio;
    }

    /** Roda a auditoria. Só processa o que está EM_ANALISE; qualquer outro status é ignorado (já terminou). */
    public void processar(Long auditoriaId) {
        try {
            Boolean emAnalise = transacao.execute(s -> auditorias.findById(auditoriaId)
                    .map(a -> a.getStatus() == StatusAuditoria.EM_ANALISE).orElse(false));
            if (!Boolean.TRUE.equals(emAnalise)) {
                return;
            }
            // Transação 1: extração.
            transacao.executeWithoutResult(s -> {
                Auditoria a = auditorias.findById(auditoriaId).orElseThrow();
                extracao.extrair(a, a.getProduto().getPaginaTexto());
            });
            // Transação 2: regras, cruzamento, pontuação, redação e relatório.
            transacao.executeWithoutResult(s -> concluir(auditorias.findById(auditoriaId).orElseThrow()));
            log.info("Auditoria {} concluída", auditoriaId);
        } catch (FalhaIaException e) {
            falhar(auditoriaId, "IA: " + e.getMessage());
        } catch (TabelaRegraVaziaException e) {
            falhar(auditoriaId, e.getMessage());
        } catch (RuntimeException e) {
            // Erro inesperado: registra o tipo (a mensagem pode trazer dados da página) e não deixa o produto preso.
            log.error("Auditoria {}: erro inesperado", auditoriaId, e);
            falhar(auditoriaId, "erro interno: " + e.getClass().getSimpleName());
        }
    }

    private void concluir(Auditoria auditoria) {
        Produto produto = auditoria.getProduto();
        String texto = produto.getPaginaTexto();
        List<ConteudoItem> entrega = conteudos.findByProdutoId(produto.getId());
        List<ItemEntrega> itens = entrega.stream().map(i -> new ItemEntrega(i.getTipo(), i.getTitulo(), i.getDescricao())).toList();
        List<AfirmacaoDetectada> detectadas = afirmacoes.findByAuditoriaId(auditoria.getId()).stream()
                .map(ProcessadorAuditoria::paraDetectada).toList();

        Set<AchadoDetectado> todos = new LinkedHashSet<>(
                motorRegras.avaliar(new ContextoRegras(preparador.dividir(texto), detectadas, itens)));
        ResultadoCruzamento cruzado = cruzamento.cruzar(auditoria, detectadas, itens);
        todos.addAll(cruzado.achados());

        Map<TipoAchado, Regra> ativas = new EnumMap<>(TipoAchado.class);
        regras.findByAtivaTrue().forEach(r -> ativas.put(r.getCodigo(), r));
        if (ativas.isEmpty()) {
            // Sem regras, todo achado seria filtrado e a pontuação daria 0 ("liberação sugerida" para tudo).
            // Melhor falhar alto do que dar um resultado bonito e errado (bug real de 26/11: data.sql não rodou).
            throw new TabelaRegraVaziaException();
        }
        List<AchadoDetectado> validos = todos.stream().filter(a -> ativas.containsKey(a.tipo())).toList();

        List<Sugestao> sugestoes = redacao.redigir(validos.stream()
                .map(a -> new ServicoRedacao.Pedido(a.tipo(), ativas.get(a.tipo()).getDescricao(), evidencia(a))).toList(),
                dadosDoProduto(produto, entrega));

        achados.deleteByAuditoriaId(auditoria.getId());
        notificacoes.deleteByAuditoriaId(auditoria.getId());
        List<MontadorRelatorio.Ponto> pontos = new ArrayList<>();
        for (int i = 0; i < validos.size(); i++) {
            AchadoDetectado a = validos.get(i);
            Regra regra = ativas.get(a.tipo());
            Achado achado = new Achado(auditoria, null, a.tipo(), regra.getSeveridade(), a.trechoPagina(), a.trechoEntrega());
            achado.setDetalhe(a.detalhe());
            achado.setSugestao(sugestoes.get(i).texto());
            achado.setSugestaoFonte(sugestoes.get(i).fonte().name());
            achados.save(achado);
            pontos.add(new MontadorRelatorio.Ponto(regra.getDescricao(), evidencia(a), sugestoes.get(i).texto()));
        }

        Map<TipoAchado, Integer> pesos = new EnumMap<>(TipoAchado.class);
        ativas.forEach((tipo, r) -> pesos.put(tipo, r.getPeso()));
        ResultadoPontuacao pontuacao = calculadora.calcular(validos.stream().map(AchadoDetectado::tipo).toList(), pesos);

        Instant agora = Instant.now(relogio);
        notificacoes.save(new NotificacaoProdutor(auditoria,
                montadorRelatorio.montar(produto.getId(), auditoria.getVersao(), pontos), agora));
        auditoria.setPontuacao(pontuacao.pontos());
        auditoria.setFaixa(pontuacao.faixa());
        auditoria.setStatus(StatusAuditoria.CONCLUIDA);
        auditoria.setConcluidaEm(agora);
        auditoria.setCruzamentoPulado(cruzado.pulado()); // CONTRATOS.md §3: sem entrega, o pulo fica registrado
        produto.setStatus(StatusProduto.AGUARDANDO_DECISAO);
    }

    private void falhar(Long auditoriaId, String motivo) {
        log.warn("Auditoria {} FALHOU ({})", auditoriaId, motivo);
        transacao.executeWithoutResult(s -> auditorias.findById(auditoriaId).ifPresent(a -> {
            a.setStatus(StatusAuditoria.FALHOU);
            a.setMotivoFalha(motivo.length() <= 2000 ? motivo : motivo.substring(0, 2000));
            a.setConcluidaEm(Instant.now(relogio));
            a.getProduto().setStatus(StatusProduto.AGUARDANDO_DECISAO);
        }));
    }

    /** O que vai para a redação e para o relatório: o trecho da página, senão o da entrega, senão a explicação. */
    static String evidencia(AchadoDetectado a) {
        if (a.trechoPagina() != null) {
            return a.trechoPagina();
        }
        return a.trechoEntrega() != null ? a.trechoEntrega() : a.detalhe();
    }

    /** Os números que a sugestão pode usar: página, entrega e preço (ConferenteNumeros). */
    private static String dadosDoProduto(Produto produto, List<ConteudoItem> entrega) {
        StringBuilder dados = new StringBuilder(produto.getPaginaTexto() == null ? "" : produto.getPaginaTexto());
        for (ConteudoItem i : entrega) {
            dados.append('\n').append(i.getTitulo()).append('\n').append(i.getDescricao() == null ? "" : i.getDescricao());
        }
        if (produto.getPreco() != null) {
            dados.append('\n').append(produto.getPreco().toPlainString());
        }
        // A contagem da entrega também é dado do produto ("a entrega tem 28 aulas").
        dados.append('\n').append(entrega.size());
        entrega.stream().map(ConteudoItem::getTipo).distinct().forEach(t ->
                dados.append('\n').append(entrega.stream().filter(i -> i.getTipo() == t).count()));
        return dados.toString();
    }

    private static AfirmacaoDetectada paraDetectada(Afirmacao a) {
        return new AfirmacaoDetectada(a.getBlocoId(), a.getTipo(), a.getTrecho());
    }
}
