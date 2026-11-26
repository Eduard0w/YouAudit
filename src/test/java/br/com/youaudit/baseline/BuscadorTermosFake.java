package br.com.youaudit.baseline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Dublê do BuscadorTermos para testar o VerificadorBaseline sem depender da implementação real: só ignora caixa
 * (sem acentos, sem palavra inteira, sem espaços variáveis). Por isso os testes que o usam têm texto sem acento.
 */
class BuscadorTermosFake extends BuscadorTermos {

    @Override
    public List<Ocorrencia> buscar(String texto, Collection<String> termos) {
        List<Ocorrencia> achadas = new ArrayList<>();
        String minusculo = texto.toLowerCase(Locale.ROOT);
        for (String termo : termos) {
            String t = termo.toLowerCase(Locale.ROOT);
            for (int i = minusculo.indexOf(t); i >= 0; i = minusculo.indexOf(t, i + 1)) {
                achadas.add(new Ocorrencia(termo, i, i + t.length(), texto.substring(i, i + t.length())));
            }
        }
        achadas.sort(Comparator.comparingInt(Ocorrencia::inicio));
        return achadas;
    }
}
