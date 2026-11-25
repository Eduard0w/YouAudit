package br.com.youaudit.coleta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import java.math.BigDecimal;

/**
 * Produto enviado para aprovação. O id vem de quem envia o evento (ex.: "sim-001"), por isso é texto e não é gerado.
 * {@code paginaTexto} guarda o texto JÁ LIMPO: é o mesmo texto usado no hash e na conferência de trecho literal.
 */
@Entity
public class Produto {

    @Id
    private String id;

    @Column(nullable = false)
    private String nome;

    private String categoria;

    /** Dinheiro sempre em BigDecimal, nunca double (double erra centavos). */
    private BigDecimal preco;

    @Lob
    private String paginaTexto;

    /** SHA-256 do texto limpo, em hexadecimal (64 caracteres). */
    @Column(length = 64)
    private String paginaHash;

    /**
     * SHA-256 da URL, quando a página veio por link. Serve para reconhecer o mesmo link de novo
     * sem guardar a URL em claro (ela pode conter o nome do produtor, regra 8).
     */
    @Column(length = 64)
    private String urlHash;

    @Enumerated(EnumType.STRING)
    private StatusProduto status;

    /** Número da auditoria mais recente (1, 2, 3...). */
    private int versaoAtual;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Origem origem;

    /** Construtor vazio exigido pelo JPA. */
    protected Produto() {
    }

    public Produto(String id) {
        this.id = id;
    }

    public String getId() { return id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }

    public String getPaginaTexto() { return paginaTexto; }
    public void setPaginaTexto(String paginaTexto) { this.paginaTexto = paginaTexto; }

    public String getPaginaHash() { return paginaHash; }
    public void setPaginaHash(String paginaHash) { this.paginaHash = paginaHash; }

    public String getUrlHash() { return urlHash; }
    public void setUrlHash(String urlHash) { this.urlHash = urlHash; }

    public StatusProduto getStatus() { return status; }
    public void setStatus(StatusProduto status) { this.status = status; }

    public int getVersaoAtual() { return versaoAtual; }
    public void setVersaoAtual(int versaoAtual) { this.versaoAtual = versaoAtual; }

    public Origem getOrigem() { return origem; }
    public void setOrigem(Origem origem) { this.origem = origem; }
}
