package br.com.youaudit.revisao;

import br.com.youaudit.regras.Achado;
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

/** Uma pessoa confirmou ou descartou um achado. Fica registrado com a nota e a data. */
@Entity
public class Revisao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "achado_id")
    private Achado achado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DecisaoRevisao decisao;

    @Column(length = 2000)
    private String nota;

    @Column(nullable = false)
    private Instant data;

    protected Revisao() {
    }

    public Revisao(Achado achado, DecisaoRevisao decisao, String nota, Instant data) {
        this.achado = achado;
        this.decisao = decisao;
        this.nota = nota;
        this.data = data;
    }

    public Long getId() { return id; }
    public Achado getAchado() { return achado; }
    public DecisaoRevisao getDecisao() { return decisao; }
    public String getNota() { return nota; }
    public Instant getData() { return data; }
}
