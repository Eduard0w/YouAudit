package br.com.youaudit.entrega;

import br.com.youaudit.evento.Auditoria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Parte da resposta do cruzamento que o código recusou (regra 2: "sem trecho válido, o alerta é descartado e
 * registrado"). Tabela separada de afirmacao_descartada para não misturar com a métrica "% de trechos literais
 * válidos" da extração.
 */
@Entity
public class DescarteCruzamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditoria_id")
    private Auditoria auditoria;

    /** ASSOCIACAO, CUSTO ou RESPOSTA (a resposta inteira). */
    @Column(nullable = false)
    private String tipo;

    @Column(length = 2000)
    private String conteudo;

    @Column(length = 500, nullable = false)
    private String motivo;

    /** 1 = primeira resposta; 2 = nova tentativa. */
    private int tentativa;

    protected DescarteCruzamento() {
    }

    public DescarteCruzamento(Auditoria auditoria, String tipo, String conteudo, String motivo, int tentativa) {
        this.auditoria = auditoria;
        this.tipo = tipo;
        this.conteudo = conteudo == null || conteudo.length() <= 2000 ? conteudo : conteudo.substring(0, 2000);
        this.motivo = motivo.length() <= 500 ? motivo : motivo.substring(0, 500);
        this.tentativa = tentativa;
    }

    public Long getId() { return id; }
    public String getTipo() { return tipo; }
    public String getConteudo() { return conteudo; }
    public String getMotivo() { return motivo; }
    public int getTentativa() { return tentativa; }
}
