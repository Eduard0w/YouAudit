package br.com.youaudit.extracao;

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
 * O que a IA devolveu e o código recusou (regra 2: "sem trecho válido, o alerta é descartado e registrado").
 * Guardar os descartes permite medir "% de trechos literais válidos" e mostrar na defesa o que foi barrado.
 * Os campos guardam o que veio da IA, como veio (por isso {@code tipo} é texto e não o enum).
 */
@Entity
public class AfirmacaoDescartada {

    static final int TAMANHO_MAXIMO = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditoria_id")
    private Auditoria auditoria;

    /** Nulo quando a resposta inteira foi descartada (ex.: JSON inválido). */
    private Integer blocoId;

    private String tipo;

    @Column(length = TAMANHO_MAXIMO)
    private String trecho;

    @Column(length = 500, nullable = false)
    private String motivo;

    /** 1 = primeira resposta da IA; 2 = a nova tentativa. */
    private int tentativa;

    protected AfirmacaoDescartada() {
    }

    public AfirmacaoDescartada(Auditoria auditoria, Integer blocoId, String tipo, String trecho, String motivo,
                               int tentativa) {
        this.auditoria = auditoria;
        this.blocoId = blocoId;
        this.tipo = cortar(tipo, 255);
        this.trecho = cortar(trecho, TAMANHO_MAXIMO);
        this.motivo = cortar(motivo, 500);
        this.tentativa = tentativa;
    }

    private static String cortar(String texto, int maximo) {
        return texto == null || texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }

    public Long getId() { return id; }
    public Auditoria getAuditoria() { return auditoria; }
    public Integer getBlocoId() { return blocoId; }
    public String getTipo() { return tipo; }
    public String getTrecho() { return trecho; }
    public String getMotivo() { return motivo; }
    public int getTentativa() { return tentativa; }
}
