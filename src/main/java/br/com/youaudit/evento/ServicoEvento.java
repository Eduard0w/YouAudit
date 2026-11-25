package br.com.youaudit.evento;

import br.com.youaudit.coleta.ConteudoItem;
import br.com.youaudit.coleta.ConteudoItemRepository;
import br.com.youaudit.coleta.LimpadorTexto;
import br.com.youaudit.coleta.Produto;
import br.com.youaudit.coleta.ProdutoRepository;
import br.com.youaudit.coleta.StatusProduto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Passo 1 do pipeline (CONTRATOS.md §4): recebe o evento, calcula o hash e decide se abre uma nova auditoria.
 *
 * <p>Idempotência: se a página (já limpa) tem o mesmo hash da ÚLTIMA auditoria do produto, e essa auditoria não
 * falhou, o evento é registrado como IGNORADO_MESMO_HASH e nada mais acontece. Comparar com a última auditoria
 * (e não com qualquer auditoria antiga) faz a página que volta a um texto anterior (A → B → A) ser auditada de novo,
 * porque a fila precisa refletir a página atual. Uma auditoria que FALHOU não bloqueia: reenviar tenta outra vez,
 * para o produto nunca ficar preso.
 */
@Service
public class ServicoEvento {

    private final LimpadorTexto limpador;
    private final CalculadoraHash calculadoraHash;
    private final ProdutoRepository produtos;
    private final ConteudoItemRepository conteudos;
    private final EventoProdutoRepository eventos;
    private final AuditoriaRepository auditorias;
    private final Clock relogio;

    public ServicoEvento(LimpadorTexto limpador, CalculadoraHash calculadoraHash, ProdutoRepository produtos,
                         ConteudoItemRepository conteudos, EventoProdutoRepository eventos,
                         AuditoriaRepository auditorias, Clock relogio) {
        this.limpador = limpador;
        this.calculadoraHash = calculadoraHash;
        this.produtos = produtos;
        this.conteudos = conteudos;
        this.eventos = eventos;
        this.auditorias = auditorias;
        this.relogio = relogio;
    }

    /** Tudo numa transação só: ou grava produto, evento e auditoria juntos, ou não grava nada. */
    @Transactional
    public EventoResposta receber(EventoProdutoRequest pedido) {
        ProdutoDoEvento dados = pedido.produto();
        String textoLimpo = limpador.limpar(dados.paginaTexto());
        if (textoLimpo.isEmpty()) {
            throw new PaginaVaziaException(dados.id());
        }
        String hash = calculadoraHash.sha256(textoLimpo);
        Instant agora = Instant.now(relogio);

        Optional<Auditoria> ultima = auditorias.findFirstByProdutoIdOrderByVersaoDesc(dados.id());
        boolean mesmaPagina = ultima
                .filter(a -> a.getHashPagina().equals(hash))
                .filter(a -> a.getStatus() != StatusAuditoria.FALHOU)
                .isPresent();

        if (mesmaPagina) {
            Produto produto = ultima.get().getProduto();
            EventoProduto evento = eventos.save(
                    new EventoProduto(produto, pedido.tipo(), hash, agora, ResultadoEvento.IGNORADO_MESMO_HASH));
            return new EventoResposta(evento.getId(), ResultadoEvento.IGNORADO_MESMO_HASH, null, null);
        }

        int versao = ultima.map(a -> a.getVersao() + 1).orElse(1);
        Produto produto = atualizarProduto(dados, textoLimpo, hash, versao);
        substituirConteudo(produto, dados.conteudo());

        Auditoria auditoria = auditorias.save(new Auditoria(produto, versao, hash, agora));
        EventoProduto evento = eventos.save(
                new EventoProduto(produto, pedido.tipo(), hash, agora, ResultadoEvento.PROCESSADO));
        return new EventoResposta(evento.getId(), ResultadoEvento.PROCESSADO, auditoria.getId(), versao);
    }

    /** Cria o produto se for novo, ou atualiza com os dados da nova versão. */
    private Produto atualizarProduto(ProdutoDoEvento dados, String textoLimpo, String hash, int versao) {
        Produto produto = produtos.findById(dados.id()).orElseGet(() -> new Produto(dados.id()));
        produto.setNome(dados.nome());
        produto.setCategoria(dados.categoria());
        produto.setPreco(dados.preco());
        produto.setOrigem(dados.origem());
        produto.setPaginaTexto(textoLimpo);
        produto.setPaginaHash(hash);
        produto.setVersaoAtual(versao);
        produto.setStatus(StatusProduto.EM_ANALISE);
        return produtos.save(produto);
    }

    /** A entrega da nova versão substitui a anterior. Sem conteúdo no evento, o produto fica sem entrega cadastrada. */
    private void substituirConteudo(Produto produto, ConteudoDoEvento conteudo) {
        conteudos.deleteByProdutoId(produto.getId());
        if (conteudo == null || conteudo.itens() == null) {
            return;
        }
        for (ItemDoEvento item : conteudo.itens()) {
            conteudos.save(new ConteudoItem(produto, item.tipo(), item.titulo(), item.descricao(), item.duracaoMin()));
        }
    }
}
