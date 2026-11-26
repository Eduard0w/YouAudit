package br.com.youaudit.regras;

import br.com.youaudit.avaliacao.AchadoDetectado;
import br.com.youaudit.avaliacao.AfirmacaoDetectada;
import br.com.youaudit.baseline.BuscadorTermos;
import br.com.youaudit.extracao.Bloco;
import br.com.youaudit.extracao.TipoAfirmacao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PROMESSA_RESULTADO_SEM_RESSALVA: a página promete resultado e não avisa, por perto, que resultados variam.
 *
 * <p>Divisão do trabalho (CONTRATOS.md §1): a IA diz QUAIS frases são promessa de resultado (tipos
 * RESULTADO_FINANCEIRO e PRAZO_RESULTADO); o código procura a ressalva. Essa é a vantagem sobre o baseline: ele só
 * reconhece promessa com palavra-chave ("fature"), a IA reconhece "transforme sua paixão em um negócio lucrativo".
 *
 * <p>"Por perto" = no mesmo bloco ou a até {@code janela} blocos antes/depois. Por que não a página inteira: um aviso
 * escondido no rodapé, longe da promessa, não cumpre o papel de avisar. A janela fica em {@code baseline-termos.json}
 * ({@code janelaRessalvaBlocos}), igual à do baseline.
 */
public class RegraPromessaSemRessalva implements RegraDeterministica {

    static final Set<TipoAfirmacao> PROMESSAS_DE_RESULTADO =
            Set.of(TipoAfirmacao.RESULTADO_FINANCEIRO, TipoAfirmacao.PRAZO_RESULTADO);

    private final List<String> palavrasRessalva;
    private final int janela;
    private final BuscadorTermos buscador;

    public RegraPromessaSemRessalva(List<String> palavrasRessalva, int janela, BuscadorTermos buscador) {
        this.palavrasRessalva = List.copyOf(palavrasRessalva);
        this.janela = janela;
        this.buscador = buscador;
    }

    @Override
    public List<AchadoDetectado> avaliar(ContextoRegras contexto) {
        Set<Integer> blocosComRessalva = new HashSet<>();
        for (Bloco bloco : contexto.blocos()) {
            if (!buscador.buscar(bloco.texto(), palavrasRessalva).isEmpty()) {
                blocosComRessalva.add(bloco.id());
            }
        }
        List<AchadoDetectado> achados = new ArrayList<>();
        for (AfirmacaoDetectada a : contexto.afirmacoes()) {
            if (PROMESSAS_DE_RESULTADO.contains(a.tipo()) && !temRessalvaPerto(a.blocoId(), blocosComRessalva)) {
                achados.add(new AchadoDetectado(TipoAchado.PROMESSA_RESULTADO_SEM_RESSALVA, a.trecho(), null,
                        "Promessa de resultado sem aviso, por perto, de que os resultados podem variar: "
                                + "possivelmente em desacordo com as regras."));
            }
        }
        return achados;
    }

    private boolean temRessalvaPerto(int blocoId, Set<Integer> blocosComRessalva) {
        for (int id = blocoId - janela; id <= blocoId + janela; id++) {
            if (blocosComRessalva.contains(id)) {
                return true;
            }
        }
        return false;
    }
}
