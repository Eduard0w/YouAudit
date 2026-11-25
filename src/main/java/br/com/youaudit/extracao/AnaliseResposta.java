package br.com.youaudit.extracao;

import java.util.List;

/**
 * O que o {@link AnalisadorRespostaExtracao} concluiu sobre UMA resposta da IA.
 *
 * @param aceitas   afirmações que passaram em todas as conferências
 * @param problemas o que foi recusado, com o motivo
 */
public record AnaliseResposta(List<Candidata> aceitas, List<Problema> problemas) {

    /** Uma afirmação aprovada pelo código, ainda não gravada. */
    public record Candidata(int blocoId, TipoAfirmacao tipo, String trecho, String valor, double confianca) {
    }

    /** Algo recusado. blocoId, tipo e trecho ficam nulos quando a resposta inteira é ilegível. */
    public record Problema(Integer blocoId, String tipo, String trecho, String motivo) {

        /** Frase curta para devolver à IA na nova tentativa. */
        public String paraCorrecao() {
            if (trecho == null) {
                return motivo;
            }
            return "bloco " + blocoId + ", trecho \"" + trecho + "\": " + motivo;
        }
    }

    public boolean totalmenteValida() {
        return problemas.isEmpty();
    }
}
