package br.com.youaudit.redacao;

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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;

import java.time.Instant;

/** Relatório "enviado" ao produtor. O envio é simulado: nada sai do sistema, só fica registrado aqui. */
@Entity
public class NotificacaoProdutor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditoria_id")
    private Auditoria auditoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CanalNotificacao canal;

    @Lob
    private String conteudo;

    @Column(nullable = false)
    private Instant enviadaEm;

    /** O produtor contestou algum ponto do relatório. */
    private boolean contestada;

    protected NotificacaoProdutor() {
    }

    public NotificacaoProdutor(Auditoria auditoria, String conteudo, Instant enviadaEm) {
        this.auditoria = auditoria;
        this.canal = CanalNotificacao.SIMULADO;
        this.conteudo = conteudo;
        this.enviadaEm = enviadaEm;
    }

    public Long getId() { return id; }
    public Auditoria getAuditoria() { return auditoria; }
    public CanalNotificacao getCanal() { return canal; }
    public String getConteudo() { return conteudo; }
    public Instant getEnviadaEm() { return enviadaEm; }

    public boolean isContestada() { return contestada; }
    public void setContestada(boolean contestada) { this.contestada = contestada; }
}
