package br.com.youaudit.coleta;

import br.com.youaudit.config.ColetaPropriedades;
import org.springframework.stereotype.Component;

/**
 * Garante um intervalo mínimo entre duas requisições quaisquer (1 por segundo, configurável).
 * É global, não por site: o protótipo é pequeno e assim fica mais simples e mais educado.
 * {@code synchronized} faz uma thread esperar a outra: duas coletas nunca saem juntas.
 */
@Component
public class LimitadorRequisicoes {

    private final long intervaloNanos;
    private long ultimaRequisicao;
    private boolean houveRequisicao;

    public LimitadorRequisicoes(ColetaPropriedades propriedades) {
        this.intervaloNanos = propriedades.intervaloMinimoMs() * 1_000_000L;
    }

    /** Bloqueia o tempo que faltar para completar o intervalo desde a última requisição. */
    public synchronized void aguardarVez() {
        if (houveRequisicao) {
            // nanoTime não volta para trás se o relógio do sistema for ajustado; serve para medir intervalos.
            long falta = ultimaRequisicao + intervaloNanos - System.nanoTime();
            if (falta > 0) {
                dormir(falta);
            }
        }
        ultimaRequisicao = System.nanoTime();
        houveRequisicao = true;
    }

    private static void dormir(long nanos) {
        try {
            Thread.sleep(nanos / 1_000_000L, (int) (nanos % 1_000_000L));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ColetaFalhouException("coleta interrompida");
        }
    }
}
