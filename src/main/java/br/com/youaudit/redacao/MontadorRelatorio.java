package br.com.youaudit.redacao;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Monta o texto do relatório de correções para o produtor (o "envio" é simulado: fica só gravado).
 * Só código: junta o que já foi validado (trechos literais e sugestões conferidas). Linguagem de indício (regra 4).
 */
@Component
public class MontadorRelatorio {

    /** Um ponto do relatório. {@code evidencia} é o trecho literal (ou a descrição da ausência). */
    public record Ponto(String regra, String evidencia, String sugestao) {
    }

    public String montar(String produtoId, int versao, List<Ponto> pontos) {
        StringBuilder t = new StringBuilder();
        t.append("Relatório de correções sugeridas — ").append(produtoId).append(", versão ").append(versao).append("\n");
        t.append("(ENVIO SIMULADO: este protótipo não envia e-mail nem mensagem.)\n\n");
        if (pontos.isEmpty()) {
            return t.append("A análise automática não encontrou pontos para ajustar. ")
                    .append("A decisão final é de uma pessoa da equipe de Operações.\n").toString();
        }
        t.append("A análise automática encontrou ").append(pontos.size())
                .append(pontos.size() == 1 ? " ponto que pode" : " pontos que podem")
                .append(" estar em desacordo com as regras ou com a entrega cadastrada. ")
                .append("São indícios para revisão, não uma decisão: uma pessoa da equipe vai analisar.\n\n");
        for (int i = 0; i < pontos.size(); i++) {
            Ponto p = pontos.get(i);
            t.append(i + 1).append(". ").append(p.regra()).append("\n");
            t.append("   Trecho: ").append(p.evidencia()).append("\n");
            t.append("   Sugestão: ").append(p.sugestao()).append("\n\n");
        }
        return t.append("Ao reenviar a página corrigida, uma nova análise é feita automaticamente.\n").toString();
    }
}
