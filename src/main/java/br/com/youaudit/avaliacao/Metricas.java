package br.com.youaudit.avaliacao;

/**
 * Contagens e as métricas derivadas delas (CONTEXTO.md §5: "em contagens e não só porcentagem").
 * Métrica com denominador zero fica NULA ("não medido"), nunca 0 nem 100: seria inventar um número.
 *
 * @param vp verdadeiros positivos (no gabarito E detectados)
 * @param fp falsos positivos (detectados, fora do gabarito)
 * @param fn falsos negativos (no gabarito, não detectados)
 */
public record Metricas(int vp, int fp, int fn, Double precisao, Double cobertura, Double f1) {

    public static Metricas de(int vp, int fp, int fn) {
        Double precisao = vp + fp == 0 ? null : (double) vp / (vp + fp);
        Double cobertura = vp + fn == 0 ? null : (double) vp / (vp + fn);
        Double f1 = precisao == null || cobertura == null || precisao + cobertura == 0
                ? null : 2 * precisao * cobertura / (precisao + cobertura);
        return new Metricas(vp, fp, fn, precisao, cobertura, f1);
    }
}
