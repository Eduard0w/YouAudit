package br.com.youaudit.entrega;

import br.com.youaudit.avaliacao.AchadoDetectado;
import br.com.youaudit.avaliacao.AfirmacaoDetectada;
import br.com.youaudit.avaliacao.ItemEntrega;
import br.com.youaudit.config.IaConfig;
import br.com.youaudit.evento.Auditoria;
import br.com.youaudit.extracao.CacheRespostasIa;
import br.com.youaudit.extracao.ClienteIa;
import br.com.youaudit.extracao.PedidoIa;
import br.com.youaudit.extracao.TipoAfirmacao;
import br.com.youaudit.regras.TipoAchado;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Passo 7 do pipeline: cruza as promessas da página com a entrega (IA associa, código valida e decide o achado).
 *
 * <p>Fluxo, igual ao da extração: cache → IA → análise; se algo for recusado, UMA nova tentativa com a lista dos
 * problemas; ficam as partes válidas da segunda; tudo o que foi recusado vai para {@code descarte_cruzamento}.
 * Só resposta 100% válida entra no cache. Falha da IA sobe ({@code FalhaIaException}) e a auditoria vira FALHOU.
 *
 * <p>Quem vira achado (decisão do CÓDIGO, não da IA):
 * <ul>
 *   <li>ELEMENTO_AUSENTE_NA_ENTREGA: promessa de bônus/certificado que a IA associou a "nenhum item". Evidência: o
 *       trecho da página (literal, já conferido na extração).</li>
 *   <li>CUSTO_OMITIDO: item da entrega com custo/requisito pago (trecho literal do item) E nenhuma afirmação
 *       CUSTO_ADICIONAL na página. Simplificação declarada: se a página avisa de QUALQUER custo, não sinaliza
 *       (o código não sabe se é o mesmo custo). O baseline faz a mesma coisa, para a comparação ser justa.</li>
 * </ul>
 * Promessa que a IA não respondeu nem na segunda tentativa NÃO vira achado: sem resposta válida, não há alerta.
 */
@Service
public class ServicoCruzamento {

    static final Set<TipoAfirmacao> PROMESSAS_DE_ENTREGA = Set.of(TipoAfirmacao.BONUS, TipoAfirmacao.CERTIFICADO);

    private final ClienteIa clienteIa;
    private final CacheRespostasIa cache;
    private final AnalisadorRespostaCruzamento analisador;
    private final DescarteCruzamentoRepository descartes;

    public ServicoCruzamento(@Qualifier(IaConfig.SECUNDARIO) ClienteIa clienteIa, CacheRespostasIa cache,
                             AnalisadorRespostaCruzamento analisador, DescarteCruzamentoRepository descartes) {
        this.clienteIa = clienteIa;
        this.cache = cache;
        this.analisador = analisador;
        this.descartes = descartes;
    }

    @Transactional
    public ResultadoCruzamento cruzar(Auditoria auditoria, List<AfirmacaoDetectada> afirmacoes, List<ItemEntrega> itens) {
        descartes.deleteByAuditoriaId(auditoria.getId());
        if (itens.isEmpty()) {
            return ResultadoCruzamento.semEntrega();
        }
        List<AfirmacaoDetectada> promessas = afirmacoes.stream().filter(a -> PROMESSAS_DE_ENTREGA.contains(a.tipo())).toList();
        String conteudo = PromptCruzamento.conteudo(promessas, itens);
        PedidoIa pedido = new PedidoIa(PromptCruzamento.INSTRUCAO_SISTEMA, conteudo, PromptCruzamento.esquema());
        String chave = cache.chave(PromptCruzamento.TAREFA, PromptCruzamento.VERSAO, clienteIa.modelo(), pedido);

        List<DescarteCruzamento> recusados = new ArrayList<>();
        int chamadas = 0;
        int doCache = 0;
        AnaliseCruzamento analise;
        Optional<String> emCache = cache.buscar(chave);
        if (emCache.isPresent()) {
            doCache++;
            analise = analisador.analisar(emCache.get(), promessas.size(), itens); // conferida de novo, sempre
            registrar(analise, auditoria, 1, recusados);
        } else {
            chamadas++;
            String primeira = clienteIa.gerarJson(pedido);
            analise = analisador.analisar(primeira, promessas.size(), itens);
            if (analise.totalmenteValida()) {
                cache.guardar(chave, primeira);
            } else {
                registrar(analise, auditoria, 1, recusados);
                List<String> problemas = analise.problemas().stream().map(AnaliseCruzamento.Problema::paraCorrecao).toList();
                PedidoIa novaTentativa = new PedidoIa(pedido.instrucaoSistema(),
                        PromptCruzamento.conteudoComCorrecao(conteudo, problemas), pedido.esquemaResposta());
                chamadas++;
                String segunda = clienteIa.gerarJson(novaTentativa);
                analise = analisador.analisar(segunda, promessas.size(), itens);
                if (analise.totalmenteValida()) {
                    cache.guardar(chave, segunda);
                }
                registrar(analise, auditoria, 2, recusados);
            }
        }
        descartes.saveAll(recusados);
        return new ResultadoCruzamento(false, achados(analise, promessas, afirmacoes), recusados.size(), chamadas, doCache);
    }

    private static List<AchadoDetectado> achados(AnaliseCruzamento analise, List<AfirmacaoDetectada> promessas,
                                                 List<AfirmacaoDetectada> todas) {
        List<AchadoDetectado> achados = new ArrayList<>();
        for (AnaliseCruzamento.Associacao a : analise.associacoes()) {
            if (a.item() == null) {
                AfirmacaoDetectada promessa = promessas.get(a.promessa());
                String oQue = promessa.tipo() == TipoAfirmacao.CERTIFICADO ? "certificado" : "bônus";
                achados.add(new AchadoDetectado(TipoAchado.ELEMENTO_AUSENTE_NA_ENTREGA, promessa.trecho(), null,
                        "A página promete " + oQue + " e não encontrei item correspondente na entrega cadastrada: "
                                + "possivelmente em desacordo com a entrega."));
            }
        }
        boolean paginaAvisaCusto = todas.stream().anyMatch(a -> a.tipo() == TipoAfirmacao.CUSTO_ADICIONAL);
        if (!paginaAvisaCusto) {
            for (AnaliseCruzamento.Custo c : analise.custos()) {
                achados.add(new AchadoDetectado(TipoAchado.CUSTO_OMITIDO, null, c.trechoEntrega(),
                        "A entrega indica um custo ou requisito pago que a página não informa: "
                                + "possivelmente em desacordo com a página."));
            }
        }
        return achados;
    }

    private static void registrar(AnaliseCruzamento analise, Auditoria auditoria, int tentativa,
                                  List<DescarteCruzamento> recusados) {
        for (AnaliseCruzamento.Problema p : analise.problemas()) {
            recusados.add(new DescarteCruzamento(auditoria, p.tipo(), p.conteudo(), p.motivo(), tentativa));
        }
    }
}
