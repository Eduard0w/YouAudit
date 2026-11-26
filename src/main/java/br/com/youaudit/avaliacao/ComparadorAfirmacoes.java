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
 * marcar 3 pedaços da mesma frase: os pedaços a mais contam como falso positivo.
 */
public final class ComparadorAfirmacoes {

    private ComparadorAfirmacoes() {
    }

    public record Contagem(int vp, int fp, int fn) {
    }

    public static Contagem comparar(List<RotuloAfirmacao> gabarito, List<AfirmacaoDetectada> detectadas) {
        List<AfirmacaoDetectada> livres = new ArrayList<>(detectadas);
        int vp = 0;
        for (RotuloAfirmacao g : gabarito) {
            for (int i = 0; i < livres.size(); i++) {
                if (casa(g.tipo(), g.trecho(), livres.get(i))) {
                    livres.remove(i);
                    vp++;
                    break;
                }
            }
        }
        return new Contagem(vp, detectadas.size() - vp, gabarito.size() - vp);
    }

    static boolean casa(TipoAfirmacao tipo, String trechoGabarito, AfirmacaoDetectada d) {
        return d.tipo() == tipo && d.trecho() != null && trechoGabarito != null
                && (trechoGabarito.contains(d.trecho()) || d.trecho().contains(trechoGabarito));
    }
}
