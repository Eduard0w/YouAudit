package br.com.youaudit.revisao;

import br.com.youaudit.coleta.Produto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Decisão de uma pessoa sobre o produto (tabela {@code decisao_produto}).
 * A classe se chama RegistroDecisao porque o nome DecisaoProduto já é o do enum LIBERAR/PEDIR_AJUSTE/ESCALAR.
 */
@Entity
@Table(name = "decisao_produto")
public class RegistroDecisao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DecisaoProduto decisao;

    /** Motivo obrigatório: toda decisão humana fica justificada na trilha. */
    @Column(length = 2000, nullable = false)
    private String motivo;

    @Column(nullable = false)
    private Instant data;

    protected RegistroDecisao() {
    }

    public RegistroDecisao(Produto produto, DecisaoProduto decisao, String motivo, Instant data) {
        this.produto = produto;
        this.decisao = decisao;
        this.motivo = motivo;
        this.data = data;
    }

    public Long getId() { return id; }
    public Produto getProduto() { return produto; }
    public DecisaoProduto getDecisao() { return decisao; }
    public String getMotivo() { return motivo; }
    public Instant getData() { return data; }
}
