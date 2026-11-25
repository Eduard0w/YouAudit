package br.com.youaudit.extracao;

import br.com.youaudit.evento.Auditoria;
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

/** Uma afirmação comercial extraída da página. Só é gravada depois de o código conferir que o trecho é literal. */
@Entity
public class Afirmacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditoria_id")
    private Auditoria auditoria;

    private int blocoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAfirmacao tipo;

    @Column(length = 2000, nullable = false)
    private String trecho;

    /** Só preenchido quando há número ou prazo (ex.: "40"). */
    private String valor;

    /** De 0 a 1, informado pela IA. É só um indicativo, não uma decisão. */
    private double confianca;

    protected Afirmacao() {
    }

    public Afirmacao(Auditoria auditoria, int blocoId, TipoAfirmacao tipo, String trecho, String valor, double confianca) {
        this.auditoria = auditoria;
        this.blocoId = blocoId;
        this.tipo = tipo;
        this.trecho = trecho;
        this.valor = valor;
        this.confianca = confianca;
    }

    public Long getId() { return id; }
    public Auditoria getAuditoria() { return auditoria; }
    public int getBlocoId() { return blocoId; }
    public TipoAfirmacao getTipo() { return tipo; }
    public String getTrecho() { return trecho; }
    public String getValor() { return valor; }
    public double getConfianca() { return confianca; }
}
