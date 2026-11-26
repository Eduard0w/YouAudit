package br.com.youaudit.avaliacao;

import java.util.List;

/** Saída de um {@link Verificador}: as afirmações que ele reconheceu e os achados que gerou. */
public record ResultadoVerificacao(String verificador, String produtoId, List<AfirmacaoDetectada> afirmacoes,
                                   List<AchadoDetectado> achados) {

    public ResultadoVerificacao {
        afirmacoes = List.copyOf(afirmacoes);
        achados = List.copyOf(achados);
    }
}
