package br.com.youaudit.regras;

import br.com.youaudit.avaliacao.AchadoDetectado;
import br.com.youaudit.baseline.BuscadorTermos;
import br.com.youaudit.baseline.Ocorrencia;
import br.com.youaudit.extracao.Bloco;

import java.util.ArrayList;
import java.util.List;

/**
 * TERMO_PROIBIDO: um termo da lista de proibições aparece na página.
 *
 * <p>Por que é igual ao baseline: a lista de termos é uma REGRA da plataforma, não inteligência. Os dois sistemas
 * usam a mesma lista ({@code baseline-termos.json}), para a comparação medir só o que a IA acrescenta.
 * Procura na página inteira (todos os blocos), não só nas afirmações, porque um termo proibido conta onde estiver.
 */
public class RegraTermoProibido implements RegraDeterministica {

    private final List<String> termos;
    private final BuscadorTermos buscador;

    public RegraTermoProibido(List<String> termos, BuscadorTermos buscador) {
        this.termos = List.copyOf(termos);
        this.buscador = buscador;
    }

    @Override
    public List<AchadoDetectado> avaliar(ContextoRegras contexto) {
        List<AchadoDetectado> achados = new ArrayList<>();
        for (Bloco bloco : contexto.blocos()) {
            for (Ocorrencia o : buscador.buscar(bloco.texto(), termos)) {
                achados.add(new AchadoDetectado(TipoAchado.TERMO_PROIBIDO, o.trecho(), null,
                        "O termo \"" + o.trecho() + "\" está na lista de proibições (hipótese de trabalho): "
                                + "possivelmente em desacordo com as regras da plataforma."));
            }
        }
        return achados;
    }
}
