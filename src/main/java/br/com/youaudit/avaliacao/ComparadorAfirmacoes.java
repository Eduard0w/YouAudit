package br.com.youaudit.avaliacao;

import br.com.youaudit.extracao.TipoAfirmacao;

import java.util.ArrayList;
import java.util.List;

/**
 * Teste A (extração): compara as afirmações de UM sistema com o gabarito de UMA página.
 *
 * <p>Critério de acerto (decisão a confirmar pelo Eduardo antes de congelar): uma afirmação detectada casa com uma
 * do gabarito se tiverem o MESMO TIPO e um trecho estiver DENTRO do outro (comparação exata, como a de trecho literal).
 * Por que "dentro" e não "igual": o gabarito marca a frase inteira ("Garantia de 7 dias para devolução") e um sistema
 * pode marcar só um pedaço dela ("Garantia de 7 dias"), ou o contrário. Os dois apontam a mesma promessa.
 *
 * <p>Cada item casa no máximo uma vez (1 para 1), na ordem do gabarito. Assim um sistema não ganha 3 acertos por
 * marcar 3 pedaços da mesma frase.
 *
 * <p><b>Pedaços agrupados (decisão do Eduardo, 26/11, opção "a"):</b> depois do 1 para 1, uma afirmação que sobrou e
 * está INTEIRA dentro do trecho de um item do gabarito que JÁ casou, com o mesmo tipo, é "agrupada": não é acerto
 * nem falso positivo. Motivo: o gabarito às vezes marca uma lista inteira numa linha só (ex.: "Bônus 1 … Bônus 6") e
 * o sistema marca um item por linha; os pedaços estão certos, só a granularidade é outra. Vale igual para o baseline
 * e para o YouAudit, e não mexe nos rótulos. Declarar no slide do comparativo.
 */
public final class ComparadorAfirmacoes {

    private ComparadorAfirmacoes() {
    }

    /** {@code agrupadas}: pedaços que caíram dentro de um item do gabarito já acertado (não entram em vp nem fp). */
    public record Contagem(int vp, int fp, int fn, int agrupadas) {
    }

    public static Contagem comparar(List<RotuloAfirmacao> gabarito, List<AfirmacaoDetectada> detectadas) {
        List<AfirmacaoDetectada> livres = new ArrayList<>(detectadas);
        List<RotuloAfirmacao> acertados = new ArrayList<>();
        int vp = 0;
        for (RotuloAfirmacao g : gabarito) {
            for (int i = 0; i < livres.size(); i++) {
                if (casa(g.tipo(), g.trecho(), livres.get(i))) {
                    livres.remove(i);
                    acertados.add(g);
                    vp++;
                    break;
                }
            }
        }
        int agrupadas = 0;
        for (int i = livres.size() - 1; i >= 0; i--) {
            AfirmacaoDetectada d = livres.get(i);
            if (acertados.stream().anyMatch(g -> g.tipo() == d.tipo() && d.trecho() != null && g.trecho() != null
                    && g.trecho().contains(d.trecho()))) {
                livres.remove(i);
                agrupadas++;
            }
        }
        return new Contagem(vp, livres.size(), gabarito.size() - vp, agrupadas);
    }

    static boolean casa(TipoAfirmacao tipo, String trechoGabarito, AfirmacaoDetectada d) {
        return d.tipo() == tipo && d.trecho() != null && trechoGabarito != null
                && (trechoGabarito.contains(d.trecho()) || d.trecho().contains(trechoGabarito));
    }
}
