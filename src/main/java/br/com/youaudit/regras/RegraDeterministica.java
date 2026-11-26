package br.com.youaudit.regras;

import br.com.youaudit.avaliacao.AchadoDetectado;

import java.util.List;

/**
 * Uma regra do passo 6 do pipeline (CONTRATOS.md §4). Regra de ouro: aqui só entra código. A IA já fez a parte dela
 * antes (classificar as afirmações); a regra só compara, conta e procura palavras.
 *
 * <p>Cada regra devolve achados com evidência literal da página (ou nenhum trecho, quando o problema é uma AUSÊNCIA).
 */
public interface RegraDeterministica {

    List<AchadoDetectado> avaliar(ContextoRegras contexto);
}
