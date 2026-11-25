package br.com.youaudit.extracao;

import br.com.youaudit.evento.Auditoria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Passos 3 a 5 do pipeline: divide a página em blocos, pede as afirmações à IA (grupo a grupo) e só grava o que
 * passou na conferência do código. O que foi recusado também é gravado, com o motivo.
 *
 * <p>Por grupo:
 * <ol>
 *   <li>resposta no cache? usa (o cache só tem respostas 100% válidas);</li>
 *   <li>senão chama a IA e analisa; se tudo válido, guarda no cache e segue;</li>
 *   <li>senão faz UMA nova tentativa, dizendo à IA o que estava errado; ficam as afirmações válidas da segunda
 *       resposta, e as recusadas das duas tentativas são registradas. O cache só guarda se a segunda for 100% válida
 *       (na chave do pedido original, para a próxima execução já achar).</li>
 * </ol>
 * Falha da IA ({@link FalhaIaException}: sem chave, sem rede, limite) não é tratada aqui: sobe para quem chamou,
 * que marca a auditoria como FALHOU. Como tudo roda numa transação, nada fica gravado pela metade.
 */
@Service
public class ServicoExtracao {

    private final PreparadorBlocos preparador;
    private final ClienteIa clienteIa;
    private final CacheRespostasIa cache;
    private final AnalisadorRespostaExtracao analisador;
    private final AfirmacaoRepository afirmacoes;
    private final AfirmacaoDescartadaRepository descartadas;

    public ServicoExtracao(PreparadorBlocos preparador, ClienteIa clienteIa, CacheRespostasIa cache,
                           AnalisadorRespostaExtracao analisador, AfirmacaoRepository afirmacoes,
                           AfirmacaoDescartadaRepository descartadas) {
        this.preparador = preparador;
        this.clienteIa = clienteIa;
        this.cache = cache;
        this.analisador = analisador;
        this.afirmacoes = afirmacoes;
        this.descartadas = descartadas;
    }

    /** Rodar de novo para a mesma auditoria substitui o resultado anterior. */
    @Transactional
    public ResultadoExtracao extrair(Auditoria auditoria, String textoLimpo) {
        afirmacoes.deleteByAuditoriaId(auditoria.getId());
        descartadas.deleteByAuditoriaId(auditoria.getId());

        List<Bloco> blocos = preparador.dividir(textoLimpo);
        Contagem contagem = new Contagem();
        List<Afirmacao> aceitas = new ArrayList<>();
        List<AfirmacaoDescartada> recusadas = new ArrayList<>();

        for (List<Bloco> grupo : preparador.agrupar(blocos)) {
            for (AnaliseResposta.Candidata c : processarGrupo(grupo, auditoria, recusadas, contagem)) {
                aceitas.add(new Afirmacao(auditoria, c.blocoId(), c.tipo(), c.trecho(), c.valor(), c.confianca()));
            }
        }
        afirmacoes.saveAll(aceitas);
        descartadas.saveAll(recusadas);
        return new ResultadoExtracao(blocos.size(), contagem.chamadas, contagem.doCache, aceitas, recusadas);
    }

    private List<AnaliseResposta.Candidata> processarGrupo(List<Bloco> grupo, Auditoria auditoria,
                                                          List<AfirmacaoDescartada> recusadas, Contagem contagem) {
        PedidoIa pedido = new PedidoIa(PromptExtracao.INSTRUCAO_SISTEMA, PromptExtracao.conteudo(grupo),
                PromptExtracao.esquema());
        String chave = cache.chave(PromptExtracao.TAREFA, PromptExtracao.VERSAO, pedido);

        Optional<String> emCache = cache.buscar(chave);
        if (emCache.isPresent()) {
            contagem.doCache++;
            // Mesmo vindo do cache, passa pela análise: o código nunca usa resposta sem conferir.
            AnaliseResposta analise = analisador.analisar(emCache.get(), grupo);
            registrar(analise, auditoria, 1, recusadas);
            return analise.aceitas();
        }

        contagem.chamadas++;
        String primeiraResposta = clienteIa.gerarJson(pedido);
        AnaliseResposta primeira = analisador.analisar(primeiraResposta, grupo);
        if (primeira.totalmenteValida()) {
            cache.guardar(chave, primeiraResposta);
            return primeira.aceitas();
        }
        registrar(primeira, auditoria, 1, recusadas);

        List<String> problemas = primeira.problemas().stream().map(AnaliseResposta.Problema::paraCorrecao).toList();
        PedidoIa novaTentativa = new PedidoIa(pedido.instrucaoSistema(),
                PromptExtracao.conteudoComCorrecao(grupo, problemas), pedido.esquemaResposta());
        contagem.chamadas++;
        String segundaResposta = clienteIa.gerarJson(novaTentativa);
        AnaliseResposta segunda = analisador.analisar(segundaResposta, grupo);
        if (segunda.totalmenteValida()) {
            cache.guardar(chave, segundaResposta);
        }
        registrar(segunda, auditoria, 2, recusadas);
        return segunda.aceitas();
    }

    private static void registrar(AnaliseResposta analise, Auditoria auditoria, int tentativa,
                                  List<AfirmacaoDescartada> recusadas) {
        for (AnaliseResposta.Problema p : analise.problemas()) {
            recusadas.add(new AfirmacaoDescartada(auditoria, p.blocoId(), p.tipo(), p.trecho(), p.motivo(), tentativa));
        }
    }

    /** Contadores mutáveis de uma execução (só para o resumo). */
    private static final class Contagem {
        private int chamadas;
        private int doCache;
    }
}
