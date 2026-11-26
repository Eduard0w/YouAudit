package br.com.youaudit.regras;

import br.com.youaudit.avaliacao.AchadoDetectado;
import br.com.youaudit.avaliacao.AfirmacaoDetectada;
import br.com.youaudit.extracao.TipoAfirmacao;

import java.util.ArrayList;
import java.util.List;

/**
 * RECONHECIMENTO_SEM_COMPROVANTE: a página cita reconhecimento oficial ("reconhecido pelo MEC") e o cadastro não tem
 * comprovante.
 *
 * <p>Hoje o cadastro do produto NÃO tem campo de comprovante (não está no CONTRATOS.md §2), então toda afirmação
 * RECONHECIMENTO_OFICIAL vira achado para uma pessoa conferir. Quando existir o campo, basta checar se está vazio.
 * A regra não diz que o reconhecimento é falso; diz que não há como conferir.
 */
public class RegraReconhecimentoSemComprovante implements RegraDeterministica {

    @Override
    public List<AchadoDetectado> avaliar(ContextoRegras contexto) {
        List<AchadoDetectado> achados = new ArrayList<>();
        for (AfirmacaoDetectada a : contexto.afirmacoes()) {
            if (a.tipo() == TipoAfirmacao.RECONHECIMENTO_OFICIAL) {
                achados.add(new AchadoDetectado(TipoAchado.RECONHECIMENTO_SEM_COMPROVANTE, a.trecho(), null,
                        "A página cita reconhecimento oficial e não há comprovante anexado para conferir."));
            }
        }
        return achados;
    }
}
