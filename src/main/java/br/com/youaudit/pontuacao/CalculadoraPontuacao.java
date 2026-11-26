package br.com.youaudit.pontuacao;

import br.com.youaudit.config.PontuacaoPropriedades;
import br.com.youaudit.regras.TipoAchado;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Passo 8 do pipeline: a pontuação de risco. Só código, nunca IA (regra de ouro).
 *
 * <p>Como calcula:
 * <ol>
 *   <li>pega os TIPOS de achado diferentes (cada regra conta UMA vez, por mais vezes que dispare);</li>
 *   <li>soma o peso de cada tipo, lido da tabela {@code regra} (só as regras ativas; inativa vale 0);</li>
 *   <li>limita a soma ao máximo (100);</li>
 *   <li>define a faixa pelos limites de {@code pontuacao.*}.</li>
 * </ol>
 *
 * <p>Por que cada regra conta uma vez: a pontuação mede QUANTOS TIPOS de problema a página tem, não quantas vezes
 * repete o mesmo. Sem isso, uma página que escreve "renda garantida" 5 vezes chegaria a 150 só por repetição,
 * e o exemplo de referência do CONTEXTO.md ("Loja Lucrativa": 20 + 15 + 25 + 30 = 90) só fecha assim.
 * Cada ocorrência continua aparecendo como achado na tela, para a pessoa revisar.
 */
@Component
public class CalculadoraPontuacao {

    private final PontuacaoPropriedades limites;

    public CalculadoraPontuacao(PontuacaoPropriedades limites) {
        this.limites = limites;
    }

    /**
     * @param tipos         os tipos dos achados da auditoria (pode ter repetidos)
     * @param pesosAtivos   peso de cada regra ATIVA (vem da tabela regra); tipo ausente = regra inativa = 0
     */
    public ResultadoPontuacao calcular(Collection<TipoAchado> tipos, Map<TipoAchado, Integer> pesosAtivos) {
        Set<TipoAchado> distintos = tipos.isEmpty() ? EnumSet.noneOf(TipoAchado.class) : EnumSet.copyOf(tipos);
        List<ResultadoPontuacao.Parcela> parcelas = new ArrayList<>();
        int soma = 0;
        for (TipoAchado tipo : distintos) { // EnumSet: ordem fixa do enum, então as parcelas saem sempre na mesma ordem
            int peso = pesosAtivos.getOrDefault(tipo, 0);
            parcelas.add(new ResultadoPontuacao.Parcela(tipo, peso));
            soma += peso;
        }
        int pontos = Math.min(soma, limites.maximo());
        return new ResultadoPontuacao(pontos, faixa(pontos), soma, parcelas);
    }

    Faixa faixa(int pontos) {
        if (pontos >= limites.inicioAlta()) {
            return Faixa.ALTA;
        }
        if (pontos >= limites.inicioMedia()) {
            return Faixa.MEDIA;
        }
        return Faixa.BAIXA;
    }
}
