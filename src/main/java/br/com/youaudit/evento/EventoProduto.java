package br.com.youaudit.evento;

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

import java.time.Instant;

/** Registro de todo evento recebido, inclusive os ignorados. É a trilha do que chegou e quando. */
@Entity
public class EventoProduto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEvento tipo;

    @Column(length = 64, nullable = false)
    private String hashPagina;

    @Column(nullable = false)
    private Instant recebidoEm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResultadoEvento resultado;

    protected EventoProduto() {
    }

    public EventoProduto(Produto produto, TipoEvento tipo, String hashPagina, Instant recebidoEm, ResultadoEvento resultado) {
        this.produto = produto;
        this.tipo = tipo;
        this.hashPagina = hashPagina;
        this.recebidoEm = recebidoEm;
        this.resultado = resultado;
    }

    public Long getId() { return id; }
    public Produto getProduto() { return produto; }
    public TipoEvento getTipo() { return tipo; }
    public String getHashPagina() { return hashPagina; }
    public Instant getRecebidoEm() { return recebidoEm; }
    public ResultadoEvento getResultado() { return resultado; }
}
