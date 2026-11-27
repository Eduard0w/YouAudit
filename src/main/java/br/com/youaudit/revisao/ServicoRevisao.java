package br.com.youaudit.revisao;

import br.com.youaudit.coleta.Produto;
import br.com.youaudit.coleta.ProdutoRepository;
import br.com.youaudit.coleta.StatusProduto;
import br.com.youaudit.regras.Achado;
import br.com.youaudit.regras.AchadoRepository;
import br.com.youaudit.regras.EstadoAchado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Passo 10: o que a PESSOA de Operações decide. Os dois métodos só registram a decisão humana; nenhum é chamado pelo
 * sistema sozinho.
 *
 * <p>Decisão de projeto: revisar um achado NÃO recalcula a pontuação. A pontuação é a estimativa do sistema (para
 * ordenar a fila); a avaliação da pessoa fica registrada ao lado (estado do achado + tabela revisao). Assim dá para
 * comparar depois o que o sistema disse com o que a pessoa concluiu.
 */
@Service
public class ServicoRevisao {

    private final AchadoRepository achados;
    private final RevisaoRepository revisoes;
    private final ProdutoRepository produtos;
    private final RegistroDecisaoRepository decisoes;
    private final Clock relogio;
    private final ServicoRegistroPromessas registroPromessas;

    public ServicoRevisao(AchadoRepository achados, RevisaoRepository revisoes, ProdutoRepository produtos,
                          RegistroDecisaoRepository decisoes, Clock relogio, ServicoRegistroPromessas registroPromessas) {
        this.achados = achados;
        this.revisoes = revisoes;
        this.produtos = produtos;
        this.decisoes = decisoes;
        this.relogio = relogio;
        this.registroPromessas = registroPromessas;
    }

    @Transactional
    public EstadoAchado revisar(Long achadoId, PedidoRevisao pedido) {
        Achado achado = achados.findById(achadoId).orElseThrow(() -> new NaoEncontradoException("achado não encontrado"));
        EstadoAchado novo = pedido.decisao() == DecisaoRevisao.CONFIRMADO ? EstadoAchado.CONFIRMADO : EstadoAchado.DESCARTADO;
        achado.setEstado(novo);
        revisoes.save(new Revisao(achado, pedido.decisao(), pedido.nota(), Instant.now(relogio)));
        return novo;
    }

    @Transactional
    public StatusProduto decidir(String produtoId, PedidoDecisao pedido) {
        Produto produto = produtos.findById(produtoId).orElseThrow(() -> new NaoEncontradoException("produto não encontrado"));
        if (produto.getStatus() == StatusProduto.EM_ANALISE) {
            throw new ConflitoException("a auditoria ainda está em análise");
        }
        StatusProduto novo = switch (pedido.decisao()) {
            case LIBERAR -> StatusProduto.LIBERADO;
            case PEDIR_AJUSTE -> StatusProduto.AJUSTE_PEDIDO;
            case ESCALAR -> StatusProduto.ESCALADO;
        };
        produto.setStatus(novo);
        Instant agora = Instant.now(relogio);
        String motivo = pedido.motivo().strip();
        decisoes.save(new RegistroDecisao(produto, pedido.decisao(), motivo, agora));
        if (pedido.decisao() == DecisaoProduto.LIBERAR) {
            // Mesma transação: se o registro falhar, a decisão também não é gravada.
            registroPromessas.congelar(produto, motivo, agora);
        }
        return novo;
    }
}
