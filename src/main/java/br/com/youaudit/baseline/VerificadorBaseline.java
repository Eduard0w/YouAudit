package br.com.youaudit.baseline;

import br.com.youaudit.avaliacao.AchadoDetectado;
import br.com.youaudit.avaliacao.AfirmacaoDetectada;
import br.com.youaudit.avaliacao.EntradaVerificacao;
import br.com.youaudit.avaliacao.ItemEntrega;
import br.com.youaudit.avaliacao.ResultadoVerificacao;
import br.com.youaudit.avaliacao.Verificador;
import br.com.youaudit.coleta.TipoConteudo;
import br.com.youaudit.extracao.Bloco;
import br.com.youaudit.extracao.PreparadorBlocos;
import br.com.youaudit.extracao.TipoAfirmacao;
import br.com.youaudit.regras.TipoAchado;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * O baseline convencional (CONTRATOS.md §7): só listas, regex e contagem. Sem IA e sem associação por sentido.
 * Não é enfraquecido de propósito: faz o melhor que essas ferramentas conseguem, para a comparação ser justa.
 *
 * <p>Usa os mesmos blocos do YouAudit ({@link PreparadorBlocos}), para o {@code blocoId} dos dois bater.
 *
 * <p>Achados que gera:
 * <ul>
 *   <li>TERMO_PROIBIDO: termo da lista, em qualquer bloco.</li>
 *   <li>PROMESSA_RESULTADO_SEM_RESSALVA: frase de RESULTADO_FINANCEIRO ou PRAZO_RESULTADO sem palavra de ressalva no
 *       mesmo bloco nem a até {@code janelaRessalvaBlocos} blocos de distância.</li>
 *   <li>RECONHECIMENTO_SEM_COMPROVANTE: toda frase de RECONHECIMENTO_OFICIAL. O cadastro não tem campo de comprovante,
 *       então o baseline não tem como saber se existe: sinaliza para uma pessoa conferir.</li>
 *   <li>Só com entrega cadastrada (sem itens, o cruzamento é pulado, como no pipeline):
 *       ELEMENTO_AUSENTE_NA_ENTREGA (bônus/certificado na página, nenhum item desse tipo na entrega),
 *       CUSTO_OMITIDO (item da entrega com palavra de custo e nenhuma frase de CUSTO_ADICIONAL na página) e
 *       QUANTIDADE_DIVERGENTE (via {@link ExtratorNumeros} e {@link ComparadorQuantidades}).</li>
 *   <li>INFO_ESSENCIAL_AUSENTE: nenhuma palavra de uma informação essencial na página inteira. Não tem trecho,
 *       porque o problema é o que falta.</li>
 * </ul>
 */
@Component
public class VerificadorBaseline implements Verificador {

    static final String NOME = "baseline";
    private static final Set<TipoAfirmacao> PROMESSAS_DE_RESULTADO =
            Set.of(TipoAfirmacao.RESULTADO_FINANCEIRO, TipoAfirmacao.PRAZO_RESULTADO);

    private final TermosBaseline termos;
    private final PreparadorBlocos preparador;
    private final BuscadorTermos buscador;
    private final ExtratorNumeros extrator;
    private final ComparadorQuantidades comparador;
    private final ClassificadorPalavraChave classificador;

    @Autowired
    public VerificadorBaseline(TermosBaseline termos, PreparadorBlocos preparador) {
        this(termos, preparador, new BuscadorTermos(), new ExtratorNumeros(termos.unidades()),
                new ComparadorQuantidades());
    }

    /** Para os testes trocarem as peças por dublês. */
    VerificadorBaseline(TermosBaseline termos, PreparadorBlocos preparador, BuscadorTermos buscador,
                        ExtratorNumeros extrator, ComparadorQuantidades comparador) {
        this.termos = termos;
        this.preparador = preparador;
        this.buscador = buscador;
        this.extrator = extrator;
        this.comparador = comparador;
        this.classificador = new ClassificadorPalavraChave(termos.palavrasChavePorTipo(), buscador);
    }

    @Override
    public String nome() {
        return NOME;
    }

    @Override
    public ResultadoVerificacao verificar(EntradaVerificacao entrada) {
        if (entrada.textoPagina().isBlank()) {
            return new ResultadoVerificacao(NOME, entrada.produtoId(), List.of(), List.of());
        }
        List<Bloco> blocos = preparador.dividir(entrada.textoPagina());
        List<AfirmacaoDetectada> afirmacoes = classificador.classificar(blocos);

        // LinkedHashSet: o mesmo achado (mesmo tipo e trechos) aparece uma vez só, na ordem em que foi achado.
        Set<AchadoDetectado> achados = new LinkedHashSet<>();
        termosProibidos(blocos, achados);
        promessasSemRessalva(blocos, afirmacoes, achados);
        reconhecimentos(afirmacoes, achados);
        if (!entrada.itens().isEmpty()) {
            elementosAusentes(afirmacoes, entrada.itens(), achados);
            custosOmitidos(afirmacoes, entrada.itens(), achados);
            achados.addAll(comparador.comparar(quantidades(blocos), entrada.itens()));
        }
        informacoesAusentes(blocos, achados);
        return new ResultadoVerificacao(NOME, entrada.produtoId(), afirmacoes, new ArrayList<>(achados));
    }

    private void termosProibidos(List<Bloco> blocos, Set<AchadoDetectado> achados) {
        for (Bloco bloco : blocos) {
            for (Ocorrencia o : buscador.buscar(bloco.texto(), termos.termosProibidos())) {
                achados.add(new AchadoDetectado(TipoAchado.TERMO_PROIBIDO, o.trecho(), null,
                        "Termo da lista de proibições (hipótese de trabalho): possivelmente em desacordo com as regras."));
            }
        }
    }

    private void promessasSemRessalva(List<Bloco> blocos, List<AfirmacaoDetectada> afirmacoes,
                                      Set<AchadoDetectado> achados) {
        Set<Integer> blocosComRessalva = new HashSet<>();
        for (Bloco bloco : blocos) {
            if (!buscador.buscar(bloco.texto(), termos.palavrasRessalva()).isEmpty()) {
                blocosComRessalva.add(bloco.id());
            }
        }
        int janela = termos.janelaRessalvaBlocos();
        for (AfirmacaoDetectada a : afirmacoes) {
            if (PROMESSAS_DE_RESULTADO.contains(a.tipo()) && !temRessalvaPerto(a.blocoId(), janela, blocosComRessalva)) {
                achados.add(new AchadoDetectado(TipoAchado.PROMESSA_RESULTADO_SEM_RESSALVA, a.trecho(), null,
                        "Promessa de resultado sem aviso de que resultados variam por perto."));
            }
        }
    }

    private static boolean temRessalvaPerto(int blocoId, int janela, Set<Integer> blocosComRessalva) {
        for (int id = blocoId - janela; id <= blocoId + janela; id++) {
            if (blocosComRessalva.contains(id)) {
                return true;
            }
        }
        return false;
    }

    private static void reconhecimentos(List<AfirmacaoDetectada> afirmacoes, Set<AchadoDetectado> achados) {
        for (AfirmacaoDetectada a : afirmacoes) {
            if (a.tipo() == TipoAfirmacao.RECONHECIMENTO_OFICIAL) {
                achados.add(new AchadoDetectado(TipoAchado.RECONHECIMENTO_SEM_COMPROVANTE, a.trecho(), null,
                        "Reconhecimento oficial citado; o cadastro não tem comprovante para conferir."));
            }
        }
    }

    private static void elementosAusentes(List<AfirmacaoDetectada> afirmacoes, List<ItemEntrega> itens,
                                          Set<AchadoDetectado> achados) {
        Map<TipoAfirmacao, TipoConteudo> prometidoParaEntregue =
                Map.of(TipoAfirmacao.BONUS, TipoConteudo.BONUS, TipoAfirmacao.CERTIFICADO, TipoConteudo.CERTIFICADO);
        for (AfirmacaoDetectada a : afirmacoes) {
            TipoConteudo esperado = prometidoParaEntregue.get(a.tipo());
            if (esperado != null && itens.stream().noneMatch(i -> i.tipo() == esperado)) {
                achados.add(new AchadoDetectado(TipoAchado.ELEMENTO_AUSENTE_NA_ENTREGA, a.trecho(), null,
                        "A página cita " + esperado.name().toLowerCase() + ", mas a entrega não tem item desse tipo."));
            }
        }
    }

    private void custosOmitidos(List<AfirmacaoDetectada> afirmacoes, List<ItemEntrega> itens,
                                Set<AchadoDetectado> achados) {
        if (afirmacoes.stream().anyMatch(a -> a.tipo() == TipoAfirmacao.CUSTO_ADICIONAL)) {
            return; // a página já avisa de algum custo; o baseline não sabe dizer se é o mesmo
        }
        for (ItemEntrega item : itens) {
            // O trecho da entrega é o campo inteiro (título ou descrição): é literal e mostra o contexto do custo.
            for (String campo : new String[]{item.titulo(), item.descricao()}) {
                if (campo != null && !buscador.buscar(campo, termos.palavrasCustoNaEntrega()).isEmpty()) {
                    achados.add(new AchadoDetectado(TipoAchado.CUSTO_OMITIDO, null, campo,
                            "A entrega cita um possível custo que a página não informa."));
                }
            }
        }
    }

    private List<QuantidadeEncontrada> quantidades(List<Bloco> blocos) {
        List<QuantidadeEncontrada> todas = new ArrayList<>();
        for (Bloco bloco : blocos) {
            todas.addAll(extrator.extrair(bloco.texto()));
        }
        return todas;
    }

    private void informacoesAusentes(List<Bloco> blocos, Set<AchadoDetectado> achados) {
        termos.informacoesEssenciais().forEach((nome, palavras) -> {
            boolean presente = blocos.stream().anyMatch(b -> !buscador.buscar(b.texto(), palavras).isEmpty());
            if (!presente) {
                achados.add(new AchadoDetectado(TipoAchado.INFO_ESSENCIAL_AUSENTE, null, null,
                        "Não encontrei na página: " + nome + "."));
            }
        });
    }
}
