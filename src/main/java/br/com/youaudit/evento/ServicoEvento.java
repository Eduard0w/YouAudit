package br.com.youaudit.evento;

import br.com.youaudit.coleta.AtribuidorIdLink;
import br.com.youaudit.coleta.ColetaFalhouException;
import br.com.youaudit.coleta.ColetorPagina;
import br.com.youaudit.coleta.ConteudoItem;
import br.com.youaudit.coleta.ConteudoItemRepository;
import br.com.youaudit.coleta.LimpadorTexto;
import br.com.youaudit.coleta.Produto;
import br.com.youaudit.coleta.ProdutoRepository;
import br.com.youaudit.coleta.ResultadoColeta;
import br.com.youaudit.coleta.StatusProduto;
import br.com.youaudit.config.ColetaPropriedades;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Passos 1 e 2 do pipeline (CONTRATOS.md §4): recebe o evento, obtém o texto da página, calcula o hash e decide
 * se abre uma nova auditoria.
 *
 * <p>Texto da página: se o evento traz URL, o {@link ColetorPagina} baixa a página e grava o snapshot, e o texto
 * do snapshot é o que vale (para o hash e, depois, para o trecho literal). Senão, usa {@code pagina_texto}.
 *
 * <p>Idempotência: se o hash é igual ao da ÚLTIMA auditoria do produto, e ela não falhou nem ficou com coleta
 * insuficiente, o evento é registrado como IGNORADO_MESMO_HASH e nada mais acontece. Comparar com a última (e não
 * com qualquer auditoria antiga) faz a página que volta a um texto anterior (A → B → A) ser auditada de novo.
 * Falha e coleta insuficiente não bloqueiam: reenviar tenta outra vez, para o produto nunca ficar preso.
 *
 * <p>Entrada LINK (formulário): o id é atribuído pelo {@link AtribuidorIdLink} ("link-NN") e o nome é o próprio id.
 *
 * <p>Auditoria EM_ANALISE criada → publica {@link AuditoriaCriada}; o {@link AgendadorAuditoria} roda o resto do
 * pipeline em segundo plano. Limite conhecido: com URL, o DOWNLOAD ainda acontece dentro da requisição (até ~11 s).
 */
@Service
public class ServicoEvento {

    private final LimpadorTexto limpador;
    private final CalculadoraHash calculadoraHash;
    private final ColetorPagina coletor;
    private final AtribuidorIdLink atribuidorIdLink;
    private final ColetaPropriedades propriedadesColeta;
    private final ProdutoRepository produtos;
    private final ConteudoItemRepository conteudos;
    private final EventoProdutoRepository eventos;
    private final AuditoriaRepository auditorias;
    private final Clock relogio;
    private final ApplicationEventPublisher publicador;

    public ServicoEvento(LimpadorTexto limpador, CalculadoraHash calculadoraHash, ColetorPagina coletor,
                         AtribuidorIdLink atribuidorIdLink, ColetaPropriedades propriedadesColeta, ProdutoRepository produtos,
                         ConteudoItemRepository conteudos, EventoProdutoRepository eventos,
                         AuditoriaRepository auditorias, Clock relogio, ApplicationEventPublisher publicador) {
        this.limpador = limpador;
        this.calculadoraHash = calculadoraHash;
        this.coletor = coletor;
        this.atribuidorIdLink = atribuidorIdLink;
        this.propriedadesColeta = propriedadesColeta;
        this.produtos = produtos;
        this.conteudos = conteudos;
        this.eventos = eventos;
        this.auditorias = auditorias;
        this.relogio = relogio;
        this.publicador = publicador;
    }

    /** Tudo numa transação só: ou grava produto, evento e auditoria juntos, ou não grava nada. */
    @Transactional
    public EventoResposta receber(EventoProdutoRequest pedido) {
        EntradaAuditoria entrada = pedido.entrada();
        String urlHash = pedido.produto().temUrl() ? calculadoraHash.sha256(pedido.produto().url().strip()) : null;
        ProdutoDoEvento dados = pedido.produto();
        if (entrada == EntradaAuditoria.LINK) {
            String id = atribuidorIdLink.idPara(urlHash);
            dados = dados.comIdENome(id, id);
        }
        Instant agora = Instant.now(relogio);
        Optional<Auditoria> ultima = auditorias.findFirstByProdutoIdOrderByVersaoDesc(dados.id());
        int versao = ultima.map(a -> a.getVersao() + 1).orElse(1);

        String textoLimpo;
        String hash;
        if (dados.temUrl()) {
            ResultadoColeta coleta;
            try {
                coleta = coletor.coletar(dados.id(), dados.url());
            } catch (ColetaFalhouException e) {
                return registrarFalhaDeColeta(pedido.tipo(), entrada, dados, urlHash, versao, agora, e.getMessage());
            }
            textoLimpo = coleta.textoLimpo();
            hash = coleta.hash();
        } else {
            textoLimpo = limpador.limpar(dados.paginaTexto());
            if (textoLimpo.isEmpty()) {
                throw new PaginaVaziaException(dados.id());
            }
            hash = calculadoraHash.sha256(textoLimpo);
        }

        if (mesmaPaginaDaUltimaAuditoria(ultima, hash)) {
            Produto produto = ultima.get().getProduto();
            EventoProduto evento = eventos.save(
                    new EventoProduto(produto, pedido.tipo(), hash, agora, ResultadoEvento.IGNORADO_MESMO_HASH));
            return new EventoResposta(evento.getId(), produto.getId(), ResultadoEvento.IGNORADO_MESMO_HASH, null, null,
                    null, entrada);
        }

        Produto produto = buscarOuCriar(dados);
        aplicarDadosDoEvento(produto, dados, urlHash, versao);
        produto.setPaginaTexto(textoLimpo);
        produto.setPaginaHash(hash);
        Auditoria auditoria = new Auditoria(produto, versao, hash, entrada, agora);

        // A regra do mínimo vale só para a coleta por URL: as páginas fictícias de pagina_texto podem ser curtas.
        if (dados.temUrl() && textoLimpo.length() < propriedadesColeta.minimoCaracteres()) {
            auditoria.setStatus(StatusAuditoria.COLETA_INSUFICIENTE);
            auditoria.setMotivoFalha("texto limpo com " + textoLimpo.length() + " caracteres; mínimo configurado: "
                    + propriedadesColeta.minimoCaracteres());
            auditoria.setConcluidaEm(agora);
            produto.setStatus(StatusProduto.AGUARDANDO_DECISAO);
        } else {
            produto.setStatus(StatusProduto.EM_ANALISE);
        }

        produtos.save(produto);
        substituirConteudo(produto, dados.conteudo());
        auditorias.save(auditoria);
        EventoProduto evento = eventos.save(
                new EventoProduto(produto, pedido.tipo(), hash, agora, ResultadoEvento.PROCESSADO));
        if (auditoria.getStatus() == StatusAuditoria.EM_ANALISE) {
            // Passos 3 a 9 rodam em segundo plano, depois do commit (AgendadorAuditoria).
            publicador.publishEvent(new AuditoriaCriada(auditoria.getId(), produto.getId(), produto.getOrigem()));
        }
        return new EventoResposta(evento.getId(), produto.getId(), ResultadoEvento.PROCESSADO, auditoria.getId(),
                versao, auditoria.getStatus(), entrada);
    }

    private static boolean mesmaPaginaDaUltimaAuditoria(Optional<Auditoria> ultima, String hash) {
        return ultima
                .filter(a -> Objects.equals(a.getHashPagina(), hash))
                .filter(a -> a.getStatus() != StatusAuditoria.FALHOU)
                .filter(a -> a.getStatus() != StatusAuditoria.COLETA_INSUFICIENTE)
                .isPresent();
    }

    /**
     * A página não pôde ser obtida: registra uma auditoria FALHOU (sem hash) com o motivo, para ficar na trilha
     * e aparecer para uma pessoa. O texto e a entrega da versão anterior, se houver, são mantidos.
     */
    private EventoResposta registrarFalhaDeColeta(TipoEvento tipo, EntradaAuditoria entrada, ProdutoDoEvento dados,
                                                  String urlHash, int versao, Instant agora, String motivo) {
        Produto produto = buscarOuCriar(dados);
        aplicarDadosDoEvento(produto, dados, urlHash, versao);
        produto.setStatus(StatusProduto.AGUARDANDO_DECISAO);
        produtos.save(produto);

        Auditoria auditoria = new Auditoria(produto, versao, null, entrada, agora);
        auditoria.setStatus(StatusAuditoria.FALHOU);
        auditoria.setMotivoFalha("coleta: " + motivo);
        auditoria.setConcluidaEm(agora);
        auditorias.save(auditoria);

        EventoProduto evento = eventos.save(
                new EventoProduto(produto, tipo, null, agora, ResultadoEvento.PROCESSADO));
        return new EventoResposta(evento.getId(), produto.getId(), ResultadoEvento.PROCESSADO, auditoria.getId(),
                versao, StatusAuditoria.FALHOU, entrada);
    }

    private Produto buscarOuCriar(ProdutoDoEvento dados) {
        return produtos.findById(dados.id()).orElseGet(() -> new Produto(dados.id()));
    }

    /**
     * Copia os dados cadastrais do evento. A URL NÃO é guardada no banco (regra 8): só o hash dela, e a URL fica
     * no snapshot, em data/real.
     */
    private static void aplicarDadosDoEvento(Produto produto, ProdutoDoEvento dados, String urlHash, int versao) {
        produto.setNome(dados.nome());
        produto.setCategoria(dados.categoria());
        produto.setPreco(dados.preco());
        produto.setOrigem(dados.origem());
        produto.setUrlHash(urlHash);
        produto.setVersaoAtual(versao);
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
