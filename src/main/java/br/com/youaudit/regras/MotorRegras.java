package br.com.youaudit.regras;

import br.com.youaudit.avaliacao.AchadoDetectado;
import br.com.youaudit.baseline.BuscadorTermos;
import br.com.youaudit.baseline.ComparadorQuantidades;
import br.com.youaudit.baseline.ExtratorNumeros;
import br.com.youaudit.baseline.TermosBaseline;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Passo 6 do pipeline: roda as 5 regras determinísticas e junta os achados.
 * (As outras duas, ELEMENTO_AUSENTE_NA_ENTREGA e CUSTO_OMITIDO, precisam da IA para associar promessa e entrega:
 * ficam no cruzamento, pacote {@code entrega}.)
 *
 * <p>As listas vêm do mesmo {@code baseline-termos.json} do baseline. Decisão: as LISTAS são regras da plataforma, e
 * o comparativo deve medir só o que a IA acrescenta (classificar frases, associar pelo sentido), não uma lista melhor.
 *
 * <p>Achados idênticos (mesmo tipo e trechos) aparecem uma vez só, na ordem em que foram achados.
 */
@Component
public class MotorRegras {

    private final List<RegraDeterministica> regras;

    public MotorRegras(TermosBaseline termos) {
        BuscadorTermos buscador = new BuscadorTermos();
        this.regras = List.of(
                new RegraTermoProibido(termos.termosProibidos(), buscador),
                new RegraPromessaSemRessalva(termos.palavrasRessalva(), termos.janelaRessalvaBlocos(), buscador),
                new RegraReconhecimentoSemComprovante(),
                new RegraQuantidadeDivergente(new ExtratorNumeros(termos.unidades()), new ComparadorQuantidades()),
                new RegraInfoEssencialAusente(termos.informacoesEssenciais(), buscador));
    }

    public List<AchadoDetectado> avaliar(ContextoRegras contexto) {
        Set<AchadoDetectado> achados = new LinkedHashSet<>();
        for (RegraDeterministica regra : regras) {
            achados.addAll(regra.avaliar(contexto));
        }
        return new ArrayList<>(achados);
    }
}
