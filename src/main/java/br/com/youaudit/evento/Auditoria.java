package br.com.youaudit.evento;

import br.com.youaudit.coleta.Produto;
import br.com.youaudit.pontuacao.Faixa;
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

/**
 * Uma rodada de auditoria de uma versão da página. Cada página diferente gera uma nova auditoria (versão 1, 2, 3...),
 * e as antigas ficam guardadas para comparar o score entre versões.
 */
@Entity
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    private int versao;

    @Column(length = 64, nullable = false)
    private String hashPagina;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAuditoria status;

    /** Nulo enquanto a auditoria não termina. */
    private Integer pontuacao;

    @Enumerated(EnumType.STRING)
    private Faixa faixa;

    @Column(nullable = false)
    private Instant criadaEm;

    private Instant concluidaEm;

    @Column(length = 2000)
    private String motivoFalha;

    protected Auditoria() {
    }

    /** Toda auditoria nasce EM_ANALISE. */
    public Auditoria(Produto produto, int versao, String hashPagina, Instant criadaEm) {
        this.produto = produto;
        this.versao = versao;
        this.hashPagina = hashPagina;
        this.criadaEm = criadaEm;
        this.status = StatusAuditoria.EM_ANALISE;
    }

    public Long getId() { return id; }
    public Produto getProduto() { return produto; }
    public int getVersao() { return versao; }
    public String getHashPagina() { return hashPagina; }

    public StatusAuditoria getStatus() { return status; }
    public void setStatus(StatusAuditoria status) { this.status = status; }

    public Integer getPontuacao() { return pontuacao; }
    public void setPontuacao(Integer pontuacao) { this.pontuacao = pontuacao; }

    public Faixa getFaixa() { return faixa; }
    public void setFaixa(Faixa faixa) { this.faixa = faixa; }

    public Instant getCriadaEm() { return criadaEm; }

    public Instant getConcluidaEm() { return concluidaEm; }
    public void setConcluidaEm(Instant concluidaEm) { this.concluidaEm = concluidaEm; }

    public String getMotivoFalha() { return motivoFalha; }
    public void setMotivoFalha(String motivoFalha) { this.motivoFalha = motivoFalha; }
}
