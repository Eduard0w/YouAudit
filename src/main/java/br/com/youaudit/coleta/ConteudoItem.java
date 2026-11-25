package br.com.youaudit.coleta;

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

/** Um item da entrega do produto (módulo, aula, bônus ou certificado). É contra isto que as promessas são conferidas. */
@Entity
public class ConteudoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** LAZY: só carrega o produto do banco se alguém pedir. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoConteudo tipo;

    private String titulo;

    @Column(length = 4000)
    private String descricao;

    /** Pode ser nulo: nem todo item tem duração. */
    private Integer duracaoMin;

    protected ConteudoItem() {
    }

    public ConteudoItem(Produto produto, TipoConteudo tipo, String titulo, String descricao, Integer duracaoMin) {
        this.produto = produto;
        this.tipo = tipo;
        this.titulo = titulo;
        this.descricao = descricao;
        this.duracaoMin = duracaoMin;
    }

    public Long getId() { return id; }
    public Produto getProduto() { return produto; }
    public TipoConteudo getTipo() { return tipo; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public Integer getDuracaoMin() { return duracaoMin; }
}
