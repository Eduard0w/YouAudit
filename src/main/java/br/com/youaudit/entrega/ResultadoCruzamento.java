package br.com.youaudit.entrega;

import br.com.youaudit.avaliacao.AchadoDetectado;

import java.util.List;

/**
 * Resultado do cruzamento com a entrega.
 *
 * @param pulado      true quando o produto não tem entrega cadastrada (CONTRATOS.md §3: "o cruzamento é pulado e
 *                    isso é registrado"); aí nenhuma chamada à IA é feita
 * @param achados     ELEMENTO_AUSENTE_NA_ENTREGA e CUSTO_OMITIDO
 * @param descartes   quantas partes da resposta da IA o código recusou (duas tentativas somadas)
 */
public record ResultadoCruzamento(boolean pulado, List<AchadoDetectado> achados, int descartes, int chamadasIa,
                                  int respostasDoCache) {

    public ResultadoCruzamento {
        achados = List.copyOf(achados);
    }

    static ResultadoCruzamento semEntrega() {
        return new ResultadoCruzamento(true, List.of(), 0, 0, 0);
    }
}
