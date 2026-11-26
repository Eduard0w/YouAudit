package br.com.youaudit.pontuacao;

import br.com.youaudit.regras.TipoAchado;

import java.util.List;

/**
 * A pontuação e de onde ela veio, para a tela mostrar a conta ("20 + 15 + 25 + 30 = 90") e não só o número.
 *
 * @param pontos       a pontuação final, já limitada ao máximo
 * @param faixa        BAIXA, MEDIA ou ALTA
 * @param somaSemTeto  a soma antes do limite (mostra quando o teto cortou algo)
 * @param parcelas     um peso por tipo de achado, cada tipo uma vez
 */
public record ResultadoPontuacao(int pontos, Faixa faixa, int somaSemTeto, List<Parcela> parcelas) {

    public ResultadoPontuacao {
        parcelas = List.copyOf(parcelas);
    }

    public record Parcela(TipoAchado tipo, int peso) {
    }
}
