package br.com.youaudit.regras;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Uma regra de auditoria e o seu peso na pontuação. Os valores iniciais vêm de {@code data.sql}:
 * os pesos ficam no banco para poderem mudar sem mexer no código.
 */
@Entity
public class Regra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private TipoAchado codigo;

    @Column(length = 500)
    private String descricao;

    private int peso;

    /**
     * Gravidade de um achado desta regra. Serve para a "liberação sugerida": faixa BAIXA e nenhum achado ALTA
     * (CONTRATOS.md §4). Valor inicial em data.sql, hipótese de trabalho.
     */
    @Enumerated(EnumType.STRING)
    private Severidade severidade;

    private boolean ativa;

    /** De onde veio a regra ou o peso. Ex.: "hipótese de trabalho" enquanto não for conferido. */
    private String fonte;

    protected Regra() {
    }

    public Long getId() { return id; }
    public TipoAchado getCodigo() { return codigo; }
    public String getDescricao() { return descricao; }
    public int getPeso() { return peso; }
    public Severidade getSeveridade() { return severidade; }
    public boolean isAtiva() { return ativa; }
    public String getFonte() { return fonte; }
}
