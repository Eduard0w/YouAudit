package br.com.youaudit.regras;

import br.com.youaudit.evento.Auditoria;
import br.com.youaudit.extracao.Afirmacao;
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

/**
 * Um alerta para revisão humana. É um indício, não um veredito.
 * Sempre tem evidência literal da página e/ou da entrega (regra 2 do CLAUDE.md).
 */
@Entity
public class Achado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditoria_id")
    private Auditoria auditoria;

    /** Nulo quando o achado não nasce de uma afirmação (ex.: INFO_ESSENCIAL_AUSENTE). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "afirmacao_id")
    private Afirmacao afirmacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAchado regraCodigo;

    @Enumerated(EnumType.STRING)
    private Severidade severidade;

    @Column(length = 2000)
    private String evidenciaPagina;

    @Column(length = 2000)
    private String evidenciaEntrega;

    /** Correção sugerida ao produtor, redigida pela IA e conferida pelo código. */
    @Column(length = 4000)
    private String sugestao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoAchado estado;

    protected Achado() {
    }

    /** Todo achado nasce PENDENTE, esperando revisão. */
    public Achado(Auditoria auditoria, Afirmacao afirmacao, TipoAchado regraCodigo, Severidade severidade,
                  String evidenciaPagina, String evidenciaEntrega) {
        this.auditoria = auditoria;
        this.afirmacao = afirmacao;
        this.regraCodigo = regraCodigo;
        this.severidade = severidade;
        this.evidenciaPagina = evidenciaPagina;
        this.evidenciaEntrega = evidenciaEntrega;
        this.estado = EstadoAchado.PENDENTE;
    }

    public Long getId() { return id; }
    public Auditoria getAuditoria() { return auditoria; }
    public Afirmacao getAfirmacao() { return afirmacao; }
    public TipoAchado getRegraCodigo() { return regraCodigo; }
    public Severidade getSeveridade() { return severidade; }
    public String getEvidenciaPagina() { return evidenciaPagina; }
    public String getEvidenciaEntrega() { return evidenciaEntrega; }

    public String getSugestao() { return sugestao; }
    public void setSugestao(String sugestao) { this.sugestao = sugestao; }

    public EstadoAchado getEstado() { return estado; }
    public void setEstado(EstadoAchado estado) { this.estado = estado; }
}
