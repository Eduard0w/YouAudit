package br.com.youaudit.avaliacao;

import br.com.youaudit.regras.TipoAchado;

import java.util.List;

/**
 * GET /api/avaliacao/comparativo: baseline × YouAudit, com REAL e SIMULADO SEPARADOS (regra 7). Nunca há uma métrica
 * que some os dois. Tudo vem de contas feitas pelo código sobre o que está no banco e nos arquivos de gabarito.
 */
public record Comparativo(Real real, Simulado simulado, List<String> avisos) {

    /**
     * Teste A, páginas reais. Só entram as páginas que TÊM saída da IA (auditoria CONCLUIDA); o baseline roda nas
     * mesmas páginas, para os dois serem medidos sobre o mesmo conjunto.
     *
     * @param trechosAceitos   afirmações que passaram na conferência de trecho literal (páginas medidas)
     * @param trechosRecusados respostas da IA recusadas pelo código (qualquer motivo) nas mesmas páginas
     * @param agrupadasBaseline afirmações do baseline que caíram dentro de um item do gabarito já acertado (não entram
     *                          em acerto nem em falso positivo; ver {@link ComparadorAfirmacoes})
     * @param agrupadasYouaudit o mesmo, para o YouAudit
     */
    public record Real(String divisao, boolean incluiOutro, List<String> paginasMedidas, List<String> paginasSemSaidaDaIa,
                       int afirmacoesNoGabarito, int outroForaDaConta, Metricas baseline, Metricas youaudit,
                       int trechosAceitos, int trechosRecusados, int agrupadasBaseline, int agrupadasYouaudit,
                       String achadosB1B2) {
    }

    /**
     * Teste B3, produtos fictícios: (produto, tipo de achado) plantado × encontrado.
     *
     * @param soYouAudit divergências plantadas que o YouAudit achou e o baseline não (com o produto e o tipo)
     */
    public record Simulado(List<String> produtosMedidos, List<String> produtosSemAuditoriaConcluida, Metricas baseline,
                           Metricas youaudit, List<Par> soYouAudit, List<Par> soBaseline) {
    }

    public record Par(String produtoId, TipoAchado tipo) {
    }
}
