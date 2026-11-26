package br.com.youaudit.baseline;

import br.com.youaudit.avaliacao.AchadoDetectado;
import br.com.youaudit.avaliacao.ItemEntrega;
import br.com.youaudit.coleta.TipoConteudo;
import br.com.youaudit.regras.TipoAchado;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Compara as quantidades da página com a contagem de itens da entrega e gera QUANTIDADE_DIVERGENTE.
 * Especificação: youaudit-docs/ESPEC-baseline-eduardo.md, peça 3.
 */
public class ComparadorQuantidades {

    private static final Map<UnidadeQuantidade, TipoConteudo> TIPO_NA_ENTREGA =
            Map.of(UnidadeQuantidade.AULA, TipoConteudo.AULA, UnidadeQuantidade.MODULO, TipoConteudo.MODULO);
    private static final Map<UnidadeQuantidade, String[]> NOME = Map.of(
            UnidadeQuantidade.AULA, new String[]{"aula", "aulas"},
            UnidadeQuantidade.MODULO, new String[]{"módulo", "módulos"});

    public List<AchadoDetectado> comparar(List<QuantidadeEncontrada> naPagina, List<ItemEntrega> entrega) {
        if (naPagina == null || naPagina.isEmpty() || entrega == null || entrega.isEmpty()) {
            return List.of();
        }
        // LinkedHashSet: a mesma quantidade repetida na página vira um achado só, na ordem da página.
        LinkedHashSet<AchadoDetectado> achados = new LinkedHashSet<>();
        for (QuantidadeEncontrada q : naPagina) {
            long cadastradas = entrega.stream().filter(i -> i.tipo() == TIPO_NA_ENTREGA.get(q.unidade())).count();
            if (cadastradas == 0) {
                continue; // a entrega não cadastrou essa unidade: ausência de cadastro não é divergência
            }
            if (diverge(q, cadastradas)) {
                achados.add(new AchadoDetectado(TipoAchado.QUANTIDADE_DIVERGENTE, q.trecho(), null, detalhe(q, cadastradas)));
            }
        }
        return new ArrayList<>(achados);
    }

    private static boolean diverge(QuantidadeEncontrada q, long cadastradas) {
        return switch (q.comparacao()) {
            case EXATO -> cadastradas != q.valor();
            case PELO_MENOS -> cadastradas < q.valor();
            case MAIS_QUE -> cadastradas <= q.valor();
        };
    }

    private static String detalhe(QuantidadeEncontrada q, long cadastradas) {
        String unidade = NOME.get(q.unidade())[cadastradas == 1 ? 0 : 1];
        return "Possivelmente em desacordo com a entrega: a página diz \"" + q.trecho()
                + "\" e a entrega cadastrada tem " + cadastradas + " " + unidade + ".";
    }
}
