package br.com.youaudit.extracao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import java.time.Instant;

/**
 * Resposta da IA guardada para não chamar a API de novo com a mesma entrada.
 * Chave = tarefa + versão do prompt + modelo + SHA-256 da entrada (ver CacheRespostasIa). Assim a demo roda mesmo com a API fora do ar.
 */
@Entity
public class CacheIa {

    @Id
    @Column(length = 200)
    private String chave;

    @Lob
    @Column(nullable = false)
    private String respostaJson;

    @Column(nullable = false)
    private Instant criadaEm;

    protected CacheIa() {
    }

    public CacheIa(String chave, String respostaJson, Instant criadaEm) {
        this.chave = chave;
        this.respostaJson = respostaJson;
        this.criadaEm = criadaEm;
    }

    public String getChave() { return chave; }
    public String getRespostaJson() { return respostaJson; }
    public Instant getCriadaEm() { return criadaEm; }
}
