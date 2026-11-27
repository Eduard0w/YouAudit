package br.com.youaudit.revisao;

import br.com.youaudit.coleta.Produto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * O que foi prometido na versão que uma pessoa LIBEROU, congelado no momento da decisão.
 *
 * <p>Por que congelar em vez de consultar depois: um alerta pode ser revisado depois do LIBERAR, e a página pode
 * mudar. Para servir de prova do que valia no dia da aprovação, o conteúdo é gravado uma vez e nunca é atualizado
 * (não há setters). {@code hashConteudo} é o SHA-256 do {@code conteudo}: na leitura, o código recalcula e compara.
 *
 * <p>Limite: quem tem acesso ao banco pode alterar o conteúdo E recalcular o hash. O hash mostra alteração acidental
 * ou ingênua; não é garantia de segurança.
 */
@Entity
@Table(name = "registro_promessas")
public class RegistroPromessas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    /** Versão da auditoria que estava valendo quando a pessoa liberou. */
    @Column(nullable = false)
    private int versao;

    /** Hash da página aprovada. Nulo se a auditoria não tinha hash. */
    @Column(length = 64)
    private String hashPagina;

    @Column(length = 2000, nullable = false)
    private String motivo;

    @Column(nullable = false)
    private Instant data;

    /** JSON com as promessas e os alertas revisados, montado pelo {@link ServicoRegistroPromessas}. */
    @Lob
    @Column(nullable = false)
    private String conteudo;

    @Column(length = 64, nullable = false)
    private String hashConteudo;

    protected RegistroPromessas() {
    }

    public RegistroPromessas(Produto produto, int versao, String hashPagina, String motivo, Instant data,
                             String conteudo, String hashConteudo) {
        this.produto = produto;
        this.versao = versao;
        this.hashPagina = hashPagina;
        this.motivo = motivo;
        this.data = data;
        this.conteudo = conteudo;
        this.hashConteudo = hashConteudo;
    }

    public Long getId() { return id; }
    public Produto getProduto() { return produto; }
    public int getVersao() { return versao; }
    public String getHashPagina() { return hashPagina; }
    public String getMotivo() { return motivo; }
    public Instant getData() { return data; }
    public String getConteudo() { return conteudo; }
    public String getHashConteudo() { return hashConteudo; }
}
