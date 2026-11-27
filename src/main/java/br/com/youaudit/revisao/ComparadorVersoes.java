package br.com.youaudit.revisao;

import br.com.youaudit.regras.EstadoAchado;
import br.com.youaudit.regras.TipoAchado;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Compara os alertas de duas versões de um produto e diz o que foi resolvido, o que continua e o que é novo.
 *
 * <p>Só compara: não muda o estado de nenhum alerta. Um alerta que continua fica PENDENTE na nova versão; a decisão
 * da pessoa na versão anterior é apenas MOSTRADA ao lado (quem decide é uma pessoa).
 *
 * <p>"Mesmo alerta" = mesmo tipo de achado e mesmos trechos (página e entrega), iguais caractere por caractere, sem
 * normalizar, como no {@code ValidadorTrechoLiteral}. Quando o alerta não tem trecho nenhum (ex.: INFO_ESSENCIAL_AUSENTE,
 * que aponta o que FALTA), compara o {@code detalhe}, que é o texto escrito pelo código dizendo o que falta.
 */
@Component
public class ComparadorVersoes {

    /** Um alerta de uma versão, já sem nada do banco (para o teste não depender de JPA). */
    public record Alerta(Long id, TipoAchado tipo, String evidenciaPagina, String evidenciaEntrega, String detalhe,
                         EstadoAchado estado, String nota) {
    }

    /** Um par da comparação. {@code anterior} é nulo para NOVO; {@code atual} é nulo para RESOLVIDO. */
    public record Item(SituacaoAlerta situacao, Alerta anterior, Alerta atual) {
    }

    /**
     * @param houvePedidoAjuste se a última decisão entre as duas versões foi PEDIR_AJUSTE. Só então um alerta
     *                          idêntico vira CONTINUA; sem pedido, vira IGUAL_ANTERIOR.
     */
    public List<Item> comparar(List<Alerta> anteriores, List<Alerta> atuais, boolean houvePedidoAjuste) {
        // Cada alerta anterior casa no máximo uma vez: se o mesmo termo aparece 2 vezes na v1 e 1 vez na v2,
        // um continua e o outro foi resolvido.
        boolean[] usado = new boolean[anteriores.size()];
        List<Item> itens = new ArrayList<>();
        SituacaoAlerta identico = houvePedidoAjuste ? SituacaoAlerta.CONTINUA : SituacaoAlerta.IGUAL_ANTERIOR;

        for (Alerta atual : atuais) {
            int par = -1;
            for (int i = 0; i < anteriores.size(); i++) {
                if (!usado[i] && mesmoAlerta(anteriores.get(i), atual)) {
                    par = i;
                    break;
                }
            }
            if (par >= 0) {
                usado[par] = true;
                itens.add(new Item(identico, anteriores.get(par), atual));
            } else {
                itens.add(new Item(SituacaoAlerta.NOVO, null, atual));
            }
        }
        for (int i = 0; i < anteriores.size(); i++) {
            if (!usado[i]) {
                itens.add(new Item(SituacaoAlerta.RESOLVIDO, anteriores.get(i), null));
            }
        }
        return itens;
    }

    static boolean mesmoAlerta(Alerta a, Alerta b) {
        if (a.tipo() != b.tipo()
                || !Objects.equals(a.evidenciaPagina(), b.evidenciaPagina())
                || !Objects.equals(a.evidenciaEntrega(), b.evidenciaEntrega())) {
            return false;
        }
        boolean semTrecho = a.evidenciaPagina() == null && a.evidenciaEntrega() == null;
        return !semTrecho || Objects.equals(a.detalhe(), b.detalhe());
    }
}
