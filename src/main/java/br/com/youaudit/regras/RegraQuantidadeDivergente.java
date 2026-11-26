package br.com.youaudit.regras;

import br.com.youaudit.avaliacao.AchadoDetectado;
import br.com.youaudit.avaliacao.AfirmacaoDetectada;
import br.com.youaudit.baseline.ComparadorQuantidades;
import br.com.youaudit.baseline.ExtratorNumeros;
import br.com.youaudit.baseline.QuantidadeEncontrada;
import br.com.youaudit.extracao.TipoAfirmacao;

import java.util.ArrayList;
import java.util.List;

/**
 * QUANTIDADE_DIVERGENTE: a página diz "40 aulas" e a entrega cadastrada tem 28.
 *
 * <p>Regra de ouro: quem lê o número e compara é o CÓDIGO ({@link ExtratorNumeros} + {@link ComparadorQuantidades},
 * as mesmas peças do baseline). O campo {@code valor} que a IA devolve NÃO é usado para comparar: seria deixar a IA
 * fazer conta. A IA só aponta quais frases falam de quantidade de conteúdo (tipo QUANTIDADE_CONTEUDO).
 *
 * <p>Sem entrega cadastrada, a regra não roda (não há com o que comparar). O trecho do achado sai do próprio trecho
 * da afirmação, então continua literal na página.
 */
public class RegraQuantidadeDivergente implements RegraDeterministica {

    private final ExtratorNumeros extrator;
    private final ComparadorQuantidades comparador;

    public RegraQuantidadeDivergente(ExtratorNumeros extrator, ComparadorQuantidades comparador) {
        this.extrator = extrator;
        this.comparador = comparador;
    }

    @Override
    public List<AchadoDetectado> avaliar(ContextoRegras contexto) {
        if (!contexto.temEntrega()) {
            return List.of();
        }
        List<QuantidadeEncontrada> quantidades = new ArrayList<>();
        for (AfirmacaoDetectada a : contexto.afirmacoes()) {
            if (a.tipo() == TipoAfirmacao.QUANTIDADE_CONTEUDO) {
                quantidades.addAll(extrator.extrair(a.trecho()));
            }
        }
        return comparador.comparar(quantidades, contexto.itens());
    }
}
